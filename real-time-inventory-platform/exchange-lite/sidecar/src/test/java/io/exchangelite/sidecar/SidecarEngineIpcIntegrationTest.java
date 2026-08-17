package io.exchangelite.sidecar;

import io.exchangelite.engine.network.LocalhostTcpIpcServer;
import io.exchangelite.engine.runtime.EngineConfig;
import io.exchangelite.engine.runtime.RuntimeCommandRegistry;
import io.exchangelite.engine.runtime.TradingEngineRuntime;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SidecarEngineIpcIntegrationTest {
    @Test
    void sidecarUsesRealTcpIpcToReachEngineRuntime() throws Exception {
        TradingEngineRuntime runtime = new TradingEngineRuntime(new EngineConfig(9090, 9191, 100, 10_000, Set.of("BTC-USD")));
        RuntimeCommandRegistry registry = new RuntimeCommandRegistry(runtime, () -> { });

        try (LocalhostTcpIpcServer ipcServer = new LocalhostTcpIpcServer(0, registry)) {
            ipcServer.start();
            SidecarConfig config = new SidecarConfig(0, "127.0.0.1", ipcServer.port(), Duration.ofSeconds(2));
            try (SidecarHttpServer sidecar = new SidecarHttpServer(config, new EngineIpcGateway(config))) {
                sidecar.start();
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("http://127.0.0.1:" + sidecar.port() + "/health"))
                        .GET()
                        .build();

                HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());

                assertEquals(200, response.statusCode());
                assertTrue(response.body().contains("\"status\":\"UP\""));
            }
        }
    }
}
