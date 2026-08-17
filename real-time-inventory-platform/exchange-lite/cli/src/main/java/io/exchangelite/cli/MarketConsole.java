package io.exchangelite.cli;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

public final class MarketConsole {
    private static final Map<String, CliCommand> COMMANDS = commands();

    private MarketConsole() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 1 || args[0].equals("help") || args[0].equals("--help")) {
            usage();
            return;
        }

        CliCommand command = COMMANDS.get(args[0]);
        if (command == null) {
            System.err.println("Unknown command: " + args[0]);
            usage();
            System.exit(2);
            return;
        }

        String baseUrl = System.getenv().getOrDefault("EXCHANGE_SIDECAR_URL", "http://127.0.0.1:8080");
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .build();
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + command.path()))
                .timeout(Duration.ofSeconds(5));
        if (command.method().equals("POST")) {
            requestBuilder.POST(HttpRequest.BodyPublishers.noBody());
        } else {
            requestBuilder.GET();
        }

        HttpResponse<String> response = send(client, requestBuilder.build());
        System.out.println(response.body());
        if (response.statusCode() >= 400) {
            System.exit(1);
        }
    }

    private static HttpResponse<String> send(HttpClient client, HttpRequest request) throws IOException, InterruptedException {
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private static void usage() {
        System.out.println("Usage: mc <command>");
        System.out.println("Commands:");
        COMMANDS.keySet().forEach(command -> System.out.println("  " + command));
    }

    private static Map<String, CliCommand> commands() {
        Map<String, CliCommand> commands = new LinkedHashMap<>();
        commands.put("stats", new CliCommand("GET", "/stats"));
        commands.put("orders", new CliCommand("GET", "/orders"));
        commands.put("markets", new CliCommand("GET", "/markets"));
        commands.put("sessions", new CliCommand("GET", "/sessions"));
        commands.put("risk", new CliCommand("GET", "/risk"));
        commands.put("heap", new CliCommand("GET", "/heap"));
        commands.put("threads", new CliCommand("GET", "/threads"));
        commands.put("config", new CliCommand("GET", "/config"));
        commands.put("reload-config", new CliCommand("POST", "/reload-config"));
        commands.put("health", new CliCommand("GET", "/health"));
        commands.put("shutdown", new CliCommand("POST", "/shutdown"));
        return Map.copyOf(commands);
    }

    private record CliCommand(String method, String path) {
    }
}
