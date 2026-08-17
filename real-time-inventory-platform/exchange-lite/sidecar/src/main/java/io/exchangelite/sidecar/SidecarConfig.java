package io.exchangelite.sidecar;

import java.time.Duration;

public record SidecarConfig(int httpPort, String engineIpcHost, int engineIpcPort, Duration engineTimeout) {
    public SidecarConfig {
        if (httpPort < 0 || engineIpcPort <= 0) {
            throw new IllegalArgumentException("sidecar port must be non-negative and IPC port must be positive");
        }
        if (engineIpcHost == null || engineIpcHost.isBlank()) {
            throw new IllegalArgumentException("engineIpcHost is required");
        }
        engineTimeout = engineTimeout == null ? Duration.ofSeconds(2) : engineTimeout;
    }

    public static SidecarConfig fromEnvironment() {
        return new SidecarConfig(
                intValue("EXCHANGE_SIDECAR_PORT", 8080),
                value("EXCHANGE_IPC_HOST", "127.0.0.1"),
                intValue("EXCHANGE_IPC_PORT", 9191),
                Duration.ofMillis(longValue("EXCHANGE_IPC_TIMEOUT_MS", 2000L))
        );
    }

    private static String value(String name, String defaultValue) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? defaultValue : value;
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
