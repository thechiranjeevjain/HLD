package com.techstudy.aggregator;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static com.techstudy.aggregator.MultiServiceAggregator.*;
import static org.junit.jupiter.api.Assertions.*;

class MultiServiceAggregatorTest {
    @Test void invokesThreeServicesConcurrently() {
        Map<String, DownstreamClient> clients = clients(
                id -> delayed("a", 150), id -> delayed("b", 150), id -> delayed("c", 150));
        long started = System.nanoTime();
        try (MultiServiceAggregator aggregator = new MultiServiceAggregator(clients, Duration.ofSeconds(1), 1, new InMemoryRepository(), 3)) {
            assertTrue(aggregator.aggregate("r1").complete());
        }
        long elapsedMillis = Duration.ofNanos(System.nanoTime() - started).toMillis();
        assertTrue(elapsedMillis < 400, "expected concurrent latency, got " + elapsedMillis + "ms");
    }

    @Test void retriesTransientFailureAndPersistsIdempotently() {
        AtomicInteger attempts = new AtomicInteger();
        InMemoryRepository repository = new InMemoryRepository();
        Map<String, DownstreamClient> clients = clients(
                id -> { if (attempts.incrementAndGet() == 1) throw new IllegalStateException("temporary"); return "ok"; },
                id -> "b", id -> "c");
        try (MultiServiceAggregator aggregator = new MultiServiceAggregator(clients, Duration.ofSeconds(1), 2, repository, 3)) {
            AggregateResponse first = aggregator.aggregate("same");
            AggregateResponse second = aggregator.aggregate("same");
            assertEquals(2, first.results().get("one").attempts());
            assertSame(first, second);
            assertEquals(1, repository.size());
        }
    }

    @Test void returnsAndPersistsPartialResultOnTimeoutAndFailure() {
        InMemoryRepository repository = new InMemoryRepository();
        Map<String, DownstreamClient> clients = clients(
                id -> "ok", id -> { throw new IllegalArgumentException("down"); }, id -> delayed("late", 3_000));
        // One second leaves enough scheduling headroom on a busy CI host while still proving a bounded timeout.
        try (MultiServiceAggregator aggregator = new MultiServiceAggregator(clients, Duration.ofSeconds(1), 1, repository, 3)) {
            AggregateResponse response = aggregator.aggregate("partial");
            assertFalse(response.complete());
            assertEquals(CallStatus.OK, response.results().get("one").status());
            assertEquals(CallStatus.ERROR, response.results().get("two").status());
            assertEquals(CallStatus.TIMEOUT, response.results().get("three").status());
            assertEquals(1, repository.size());
        }
    }

    private Map<String, DownstreamClient> clients(DownstreamClient one, DownstreamClient two, DownstreamClient three) {
        Map<String, DownstreamClient> clients = new LinkedHashMap<>(); clients.put("one", one); clients.put("two", two); clients.put("three", three); return clients;
    }
    private static String delayed(String value, long millis) throws InterruptedException { Thread.sleep(millis); return value; }
}
