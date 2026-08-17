package io.exchangelite.engine.runtime;

import io.exchangelite.common.ipc.RuntimeCommand;
import io.exchangelite.common.ipc.RuntimeCommandType;
import io.exchangelite.common.ipc.RuntimeResponse;

import java.util.Objects;

public final class RuntimeCommandRegistry {
    private final TradingEngineRuntime runtime;
    private final Runnable shutdownHook;

    public RuntimeCommandRegistry(TradingEngineRuntime runtime, Runnable shutdownHook) {
        this.runtime = Objects.requireNonNull(runtime, "runtime");
        this.shutdownHook = shutdownHook == null ? () -> { } : shutdownHook;
    }

    public RuntimeResponse handle(RuntimeCommand command) {
        runtime.metrics().recordIpcCommand();
        try {
            return switch (command.type()) {
                case STATS -> RuntimeResponse.ok(runtime.statsJson());
                case ORDERS -> RuntimeResponse.ok(runtime.ordersJson());
                case MARKETS -> RuntimeResponse.ok(runtime.marketsJson());
                case SESSIONS -> RuntimeResponse.ok(runtime.sessionsJson());
                case RISK -> RuntimeResponse.ok(runtime.riskJson());
                case HEAP -> RuntimeResponse.ok(runtime.heapJson());
                case THREADS -> RuntimeResponse.ok(runtime.threadsJson());
                case CONFIG -> RuntimeResponse.ok(runtime.configJson());
                case RELOAD_CONFIG -> RuntimeResponse.ok(runtime.reloadConfigJson());
                case HEALTH -> RuntimeResponse.ok(runtime.healthJson());
                case METRICS -> RuntimeResponse.ok(runtime.prometheusText());
                case SHUTDOWN -> {
                    shutdownHook.run();
                    yield RuntimeResponse.accepted("{\"shutdown\":\"accepted\"}");
                }
            };
        } catch (RuntimeException ex) {
            return RuntimeResponse.error(500, "{\"error\":" + EngineJson.quote(ex.getMessage()) + "}");
        }
    }

    public boolean supports(RuntimeCommandType type) {
        return type != null;
    }
}
