package io.exchangelite.common.metrics;

import java.util.concurrent.atomic.LongAdder;

public final class ExchangeMetrics {
    private final LongAdder ordersAccepted = new LongAdder();
    private final LongAdder ordersRejected = new LongAdder();
    private final LongAdder ordersCancelled = new LongAdder();
    private final LongAdder tradesExecuted = new LongAdder();
    private final LongAdder bytesRead = new LongAdder();
    private final LongAdder ipcCommands = new LongAdder();
    private final LongAdder matchingLatencyNanos = new LongAdder();

    public void recordAcceptedOrder(long latencyNanos) {
        ordersAccepted.increment();
        matchingLatencyNanos.add(Math.max(0, latencyNanos));
    }

    public void recordRejectedOrder() {
        ordersRejected.increment();
    }

    public void recordCancelledOrder() {
        ordersCancelled.increment();
    }

    public void recordTrade() {
        tradesExecuted.increment();
    }

    public void recordBytesRead(long bytes) {
        bytesRead.add(Math.max(0, bytes));
    }

    public void recordIpcCommand() {
        ipcCommands.increment();
    }

    public MetricsSnapshot snapshot() {
        return new MetricsSnapshot(
                ordersAccepted.sum(),
                ordersRejected.sum(),
                ordersCancelled.sum(),
                tradesExecuted.sum(),
                bytesRead.sum(),
                ipcCommands.sum(),
                matchingLatencyNanos.sum()
        );
    }
}
