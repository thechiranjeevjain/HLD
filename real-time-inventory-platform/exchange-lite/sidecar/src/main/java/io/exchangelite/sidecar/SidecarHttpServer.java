package io.exchangelite.sidecar;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import io.exchangelite.common.ipc.RuntimeCommand;
import io.exchangelite.common.ipc.RuntimeCommandType;
import io.exchangelite.common.ipc.RuntimeResponse;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class SidecarHttpServer implements AutoCloseable {
    private final SidecarConfig config;
    private final IpcGateway gateway;
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private HttpServer server;

    public SidecarHttpServer(SidecarConfig config, IpcGateway gateway) {
        this.config = config;
        this.gateway = gateway;
    }

    public void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress(config.httpPort()), 0);
        server.setExecutor(executor);
        server.createContext("/", this::handle);
        server.start();
    }

    public int port() {
        if (server == null) {
            throw new IllegalStateException("server not started");
        }
        return server.getAddress().getPort();
    }

    @Override
    public void close() {
        if (server != null) {
            server.stop(1);
        }
        executor.shutdownNow();
    }

    private void handle(HttpExchange exchange) throws IOException {
        Route route = Route.find(exchange.getRequestMethod(), exchange.getRequestURI().getPath());
        if (route == null) {
            send(exchange, 404, "application/json", "{\"error\":\"unknown sidecar route\"}");
            return;
        }

        try {
            RuntimeResponse response = gateway.execute(RuntimeCommand.of(route.commandType()));
            String contentType = route.commandType() == RuntimeCommandType.METRICS ? "text/plain" : "application/json";
            send(exchange, response.statusCode(), contentType, response.body());
        } catch (IOException ex) {
            String body = "{\"error\":\"engine IPC unavailable\",\"message\":" + quote(ex.getMessage()) + "}";
            send(exchange, 502, "application/json", body);
        } catch (RuntimeException ex) {
            String body = "{\"error\":\"sidecar failure\",\"message\":" + quote(ex.getMessage()) + "}";
            send(exchange, 500, "application/json", body);
        }
    }

    private void send(HttpExchange exchange, int statusCode, String contentType, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", contentType + "; charset=utf-8");
        exchange.getResponseHeaders().set("Cache-Control", "no-store");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream output = exchange.getResponseBody()) {
            output.write(bytes);
        }
    }

    private String quote(String value) {
        if (value == null) {
            return "\"\"";
        }
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    private record Route(String method, String path, RuntimeCommandType commandType) {
        private static final Map<String, Route> ROUTES = routes();

        private static Route find(String method, String path) {
            return ROUTES.get(method.toUpperCase() + " " + normalize(path));
        }

        private static Map<String, Route> routes() {
            Map<String, Route> routes = new LinkedHashMap<>();
            register(routes, "GET", "/health", RuntimeCommandType.HEALTH);
            register(routes, "GET", "/stats", RuntimeCommandType.STATS);
            register(routes, "GET", "/orders", RuntimeCommandType.ORDERS);
            register(routes, "GET", "/markets", RuntimeCommandType.MARKETS);
            register(routes, "GET", "/sessions", RuntimeCommandType.SESSIONS);
            register(routes, "GET", "/risk", RuntimeCommandType.RISK);
            register(routes, "GET", "/heap", RuntimeCommandType.HEAP);
            register(routes, "GET", "/threads", RuntimeCommandType.THREADS);
            register(routes, "GET", "/config", RuntimeCommandType.CONFIG);
            register(routes, "POST", "/reload-config", RuntimeCommandType.RELOAD_CONFIG);
            register(routes, "POST", "/shutdown", RuntimeCommandType.SHUTDOWN);
            register(routes, "GET", "/metrics", RuntimeCommandType.METRICS);
            return Map.copyOf(routes);
        }

        private static void register(Map<String, Route> routes, String method, String path, RuntimeCommandType type) {
            Route route = new Route(method, path, type);
            routes.put(method + " " + path, route);
        }

        private static String normalize(String path) {
            if (path == null || path.isBlank()) {
                return "/";
            }
            if (path.length() > 1 && path.endsWith("/")) {
                return path.substring(0, path.length() - 1);
            }
            return path;
        }
    }
}
