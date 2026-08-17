package io.exchangelite.engine.runtime;

import java.util.Set;

public record EngineConfig(
        int dataPlanePort,
        int ipcPort,
        int maxOrderQuantity,
        long maxOrderNotionalTicks,
        Set<String> markets
) {
    public EngineConfig {
        if (dataPlanePort <= 0 || ipcPort <= 0) {
            throw new IllegalArgumentException("ports must be positive");
        }
        if (maxOrderQuantity <= 0) {
            throw new IllegalArgumentException("maxOrderQuantity must be positive");
        }
        if (maxOrderNotionalTicks <= 0) {
            throw new IllegalArgumentException("maxOrderNotionalTicks must be positive");
        }
        markets = Set.copyOf(markets);
    }

    public static EngineConfig fromEnvironment() {
        return new EngineConfig(
                intValue("EXCHANGE_DATA_PORT", 9090),
                intValue("EXCHANGE_IPC_PORT", 9191),
                intValue("EXCHANGE_MAX_ORDER_QTY", 1_000_000),
                longValue("EXCHANGE_MAX_ORDER_NOTIONAL", 1_000_000_000_000L),
                Set.of("BTC-USD", "ETH-USD")
        );
    }

    public String json() {
        return "{"
                + "\"dataPlanePort\":" + dataPlanePort + ","
                + "\"ipcPort\":" + ipcPort + ","
                + "\"maxOrderQuantity\":" + maxOrderQuantity + ","
                + "\"maxOrderNotionalTicks\":" + maxOrderNotionalTicks + ","
                + "\"markets\":" + markets.stream().map(EngineJson::quote).toList()
                + "}";
    }

    private static int intValue(String name, int defaultValue) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? defaultValue : Integer.parseInt(value);
    }

    private static long longValue(String name, long defaultValue) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? defaultValue : Long.parseLong(value);
    }
}
