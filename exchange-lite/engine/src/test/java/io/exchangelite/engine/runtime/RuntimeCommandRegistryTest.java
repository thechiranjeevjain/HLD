package io.exchangelite.engine.runtime;

import io.exchangelite.common.ipc.RuntimeCommand;
import io.exchangelite.common.ipc.RuntimeCommandType;
import io.exchangelite.common.ipc.RuntimeResponse;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RuntimeCommandRegistryTest {
    @Test
    void exposesHealthAndStats() {
        TradingEngineRuntime runtime = new TradingEngineRuntime(new EngineConfig(9090, 9191, 100, 10_000, Set.of("BTC-USD")));
        RuntimeCommandRegistry registry = new RuntimeCommandRegistry(runtime, () -> { });

        RuntimeResponse health = registry.handle(RuntimeCommand.of(RuntimeCommandType.HEALTH));
        RuntimeResponse stats = registry.handle(RuntimeCommand.of(RuntimeCommandType.STATS));

        assertEquals(200, health.statusCode());
        assertTrue(health.body().contains("\"status\":\"UP\""));
        assertEquals(200, stats.statusCode());
        assertTrue(stats.body().contains("\"openOrders\""));
    }

    @Test
    void shutdownInvokesHook() {
        TradingEngineRuntime runtime = new TradingEngineRuntime(new EngineConfig(9090, 9191, 100, 10_000, Set.of("BTC-USD")));
        AtomicBoolean called = new AtomicBoolean(false);
        RuntimeCommandRegistry registry = new RuntimeCommandRegistry(runtime, () -> called.set(true));

        RuntimeResponse response = registry.handle(RuntimeCommand.of(RuntimeCommandType.SHUTDOWN));

        assertEquals(202, response.statusCode());
        assertTrue(called.get());
    }
}
