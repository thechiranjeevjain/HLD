package io.exchangelite.common.metrics;

public record MetricsSnapshot(
        long ordersAccepted,
        long ordersRejected,
        long ordersCancelled,
        long tradesExecuted,
        long bytesRead,
        long ipcCommands,
        long matchingLatencyNanos
) {
}
