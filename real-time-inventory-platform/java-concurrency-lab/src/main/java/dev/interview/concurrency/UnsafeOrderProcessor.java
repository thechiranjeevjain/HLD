package dev.interview.concurrency;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

/** Deliberately broken. Never copy these patterns into production code. */
public final class UnsafeOrderProcessor {
    private final Map<String, Long> exposures = new HashMap<>();
    private long processed;

    public void process(Order order) {
        long before = exposures.getOrDefault(order.clientId(), 0L); // read
        Thread.yield();                                             // widens race window
        exposures.put(order.clientId(), before + order.signedNotionalCents()); // write
        processed++;                                                // another lost update
    }

    /** A teaching hook that deterministically makes all workers overwrite the same value. */
    public void processWithReadBarrier(Order order, CountDownLatch allRead, CountDownLatch releaseWrites) {
        long before = exposures.getOrDefault(order.clientId(), 0L);
        allRead.countDown();
        try { releaseWrites.await(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); return; }
        exposures.put(order.clientId(), before + order.signedNotionalCents());
        processed++;
    }

    public long exposure(String clientId) { return exposures.getOrDefault(clientId, 0L); }
    public long processed() { return processed; }
    public int clientCount() { return exposures.size(); }
}
