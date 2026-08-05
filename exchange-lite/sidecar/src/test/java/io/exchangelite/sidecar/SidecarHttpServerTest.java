package io.exchangelite.sidecar;

import io.exchangelite.common.ipc.RuntimeCommand;
import io.exchangelite.common.ipc.RuntimeCommandType;
import io.exchangelite.common.ipc.RuntimeResponse;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SidecarHttpServerTest {
    @Test
    void translatesStatsRouteIntoIpcCommand() throws Exception {
        AtomicReference<RuntimeCommandType> capturedType = new AtomicReference<>();
        IpcGateway gateway = command -> {
            capturedType.set(command.type());
            return RuntimeResponse.ok("{\"ok\":true}");
        };

        try (SidecarHttpServer server = new SidecarHttpServer(
                new SidecarConfig(0, "127.0.0.1", 9191, Duration.ofSeconds(1)),
                gateway)) {
            server.start();
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://127.0.0.1:" + server.port() + "/stats"))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            assertEquals(200, response.statusCode());
            assertEquals(RuntimeCommandType.STATS, capturedType.get());
            assertTrue(response.body().contains("\"ok\":true"));
        }
    }
}
