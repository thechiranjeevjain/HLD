package dev.interview.concurrency;

import static org.junit.jupiter.api.Assertions.*;
import java.time.Duration;
import java.util.ArrayList;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;

class ConcurrencyFailuresTest {
    @Test void lostUpdatesAreReproducedDeterministically() throws Exception {
        int workers = 12; var unsafe = new UnsafeOrderProcessor();
        var allRead = new CountDownLatch(workers); var release = new CountDownLatch(1);
        try (var pool = Executors.newFixedThreadPool(workers)) {
            for (int i = 0; i < workers; i++) pool.submit(() -> unsafe.processWithReadBarrier(order(1, Order.Side.BUY), allRead, release));
            assertTrue(allRead.await(2, TimeUnit.SECONDS)); release.countDown(); pool.shutdown(); assertTrue(pool.awaitTermination(2, TimeUnit.SECONDS));
        }
        assertEquals(1_000, unsafe.exposure("C"), "all workers overwrite the same stale read");
        assertNotEquals(workers * 1_000L, unsafe.exposure("C"));
    }

    @Test void exposureLimitInvariantSurvivesConcurrency() {
        long limit = 10_000; int orders = 1_000;
        try (var processor = new ConcurrentOrderProcessor(8, 32, limit)) {
            var futures = new ArrayList<CompletableFuture<ConcurrentOrderProcessor.Result>>();
            for (int i = 0; i < orders; i++) futures.add(processor.submit(order(i, Order.Side.BUY)));
            CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();
            assertTrue(Math.abs(processor.state("C").netExposureCents()) <= limit);
            assertEquals(orders, processor.accepted() + processor.rejected());
            assertEquals(10, processor.accepted());
        }
    }

    @Test void oppositeLockOrderingCreatesDetectableDeadlockWithoutHangingTest() throws Exception {
        assertTrue(FailureDemos.deadlockDetected(Duration.ofSeconds(2)));
    }

    @Test void nestedSubmissionToSameSingleThreadPoolStarves() throws Exception {
        assertTrue(FailureDemos.starvationDetected());
    }

    @Test void boundedQueueAndCallerRunsProvideBackpressure() throws Exception {
        assertTrue(FailureDemos.unboundedOverloadQueueSize(500) >= 498);
        assertTrue(FailureDemos.boundedCallerRunsAppliesBackpressure());
    }

    private static Order order(long id, Order.Side side) { return new Order(id, "C", "AAPL", side, 1, 1_000); }
}
