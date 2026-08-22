package com.techstudy.aggregator;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Concurrent fan-out/fan-in service with bounded calls, partial results and idempotent persistence. */
public final class MultiServiceAggregator implements AutoCloseable {
    public enum CallStatus { OK, TIMEOUT, ERROR }
    public record CallOutcome(String service, CallStatus status, String value, int attempts, String detail) {
        static CallOutcome timeout(String service) { return new CallOutcome(service, CallStatus.TIMEOUT, "", 0, "deadline exceeded"); }
    }
    public record AggregateResponse(String requestId, Map<String, CallOutcome> results, boolean complete, Instant completedAt) {}

    @FunctionalInterface
    public interface DownstreamClient {
        String fetch(String requestId) throws Exception;
    }

    public interface AggregateRepository {
        Optional<AggregateResponse> find(String requestId);
        AggregateResponse saveIfAbsent(AggregateResponse response);
    }

    public static final class InMemoryRepository implements AggregateRepository {
        private final Map<String, AggregateResponse> values = new LinkedHashMap<>();
        @Override public synchronized Optional<AggregateResponse> find(String requestId) { return Optional.ofNullable(values.get(requestId)); }
        @Override public synchronized AggregateResponse saveIfAbsent(AggregateResponse response) {
            return values.computeIfAbsent(response.requestId(), ignored -> response);
        }
        public synchronized int size() { return values.size(); }
    }

    public static final class JsonLinesRepository implements AggregateRepository {
        private final Path path;
        private final Map<String, AggregateResponse> values = new LinkedHashMap<>();

        public JsonLinesRepository(Path path) { this.path = path; }
        @Override public synchronized Optional<AggregateResponse> find(String requestId) { return Optional.ofNullable(values.get(requestId)); }
        @Override public synchronized AggregateResponse saveIfAbsent(AggregateResponse response) {
            AggregateResponse existing = values.get(response.requestId());
            if (existing != null) return existing;
            values.put(response.requestId(), response);
            try {
                Path parent = path.toAbsolutePath().getParent();
                if (parent != null) Files.createDirectories(parent);
                Files.writeString(path, toJson(response) + System.lineSeparator(), StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } catch (IOException e) { throw new IllegalStateException("could not persist aggregate", e); }
            return response;
        }
    }

    private final Map<String, DownstreamClient> clients;
    private final Duration timeout;
    private final int maxAttempts;
    private final AggregateRepository repository;
    private final ExecutorService executor;

    public MultiServiceAggregator(Map<String, DownstreamClient> clients, Duration timeout, int maxAttempts,
                                  AggregateRepository repository, int workerThreads) {
        if (clients.size() != 3) throw new IllegalArgumentException("interview scenario requires exactly three downstreams");
        this.clients = new LinkedHashMap<>(clients);
        this.timeout = timeout;
        this.maxAttempts = maxAttempts;
        this.repository = repository;
        this.executor = Executors.newFixedThreadPool(workerThreads);
    }

    public AggregateResponse aggregate(String requestId) {
        Objects.requireNonNull(requestId);
        Optional<AggregateResponse> previous = repository.find(requestId);
        if (previous.isPresent()) return previous.get();

        Map<String, CompletableFuture<CallOutcome>> calls = new LinkedHashMap<>();
        clients.forEach((name, client) -> calls.put(name,
                CompletableFuture.supplyAsync(() -> callWithRetry(name, client, requestId), executor)
                        .completeOnTimeout(CallOutcome.timeout(name), timeout.toMillis(), java.util.concurrent.TimeUnit.MILLISECONDS)));
        CompletableFuture.allOf(calls.values().toArray(CompletableFuture[]::new)).join();

        Map<String, CallOutcome> outcomes = new LinkedHashMap<>();
        calls.forEach((name, future) -> outcomes.put(name, future.join()));
        boolean complete = outcomes.values().stream().allMatch(o -> o.status() == CallStatus.OK);
        return repository.saveIfAbsent(new AggregateResponse(requestId, Map.copyOf(outcomes), complete, Instant.now()));
    }

    private CallOutcome callWithRetry(String name, DownstreamClient client, String requestId) {
        Exception last = null;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try { return new CallOutcome(name, CallStatus.OK, client.fetch(requestId), attempt, "success"); }
            catch (Exception e) { last = e; }
        }
        return new CallOutcome(name, CallStatus.ERROR, "", maxAttempts,
                last == null ? "unknown error" : last.getClass().getSimpleName() + ": " + last.getMessage());
    }

    public static HttpServer startHttpServer(int port, MultiServiceAggregator aggregator) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/aggregate", exchange -> handle(exchange, aggregator));
        server.setExecutor(Executors.newFixedThreadPool(8));
        server.start();
        return server;
    }

    private static void handle(HttpExchange exchange, MultiServiceAggregator aggregator) throws IOException {
        if (!"GET".equals(exchange.getRequestMethod())) { send(exchange, 405, "{\"error\":\"GET required\"}"); return; }
        String requestId = query(exchange, "requestId").orElse("");
        if (requestId.isBlank()) { send(exchange, 400, "{\"error\":\"requestId required\"}"); return; }
        AggregateResponse response = aggregator.aggregate(requestId);
        send(exchange, response.complete() ? 200 : 206, toJson(response));
    }

    private static Optional<String> query(HttpExchange exchange, String key) {
        String raw = exchange.getRequestURI().getRawQuery();
        if (raw == null) return Optional.empty();
        for (String pair : raw.split("&")) {
            String[] parts = pair.split("=", 2);
            if (parts.length == 2 && URLDecoder.decode(parts[0], StandardCharsets.UTF_8).equals(key))
                return Optional.of(URLDecoder.decode(parts[1], StandardCharsets.UTF_8));
        }
        return Optional.empty();
    }

    private static void send(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }

    private static String toJson(AggregateResponse response) {
        List<String> items = new ArrayList<>();
        response.results().forEach((name, result) -> items.add("\"" + escape(name) + "\":{\"status\":\"" + result.status()
                + "\",\"value\":\"" + escape(result.value()) + "\",\"attempts\":" + result.attempts()
                + ",\"detail\":\"" + escape(result.detail()) + "\"}"));
        return "{\"requestId\":\"" + escape(response.requestId()) + "\",\"complete\":" + response.complete()
                + ",\"completedAt\":\"" + response.completedAt() + "\",\"results\":{" + String.join(",", items) + "}}";
    }

    private static String escape(String value) { return value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n"); }
    @Override public void close() { executor.shutdownNow(); }

    public static void main(String[] args) throws Exception {
        Map<String, DownstreamClient> clients = new LinkedHashMap<>();
        clients.put("profile", id -> { Thread.sleep(80); return "customer:" + id; });
        clients.put("pricing", id -> { Thread.sleep(120); return "price:42.50"; });
        clients.put("inventory", id -> { Thread.sleep(50); return "available:true"; });
        MultiServiceAggregator aggregator = new MultiServiceAggregator(clients, Duration.ofMillis(300), 2,
                new JsonLinesRepository(Path.of("data", "aggregates.jsonl")), 6);
        if (args.length > 0 && "server".equalsIgnoreCase(args[0])) {
            startHttpServer(8080, aggregator);
            System.out.println("Aggregator listening on http://localhost:8080/aggregate?requestId=demo-1");
        } else {
            System.out.println(toJson(aggregator.aggregate("demo-1")));
            aggregator.close();
        }
    }
}
