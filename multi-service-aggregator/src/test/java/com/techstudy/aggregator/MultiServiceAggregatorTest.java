package com.techstudy.aggregator;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static com.techstudy.aggregator.MultiServiceAggregator.*;
import static org.junit.jupiter.api.Assertions.*;

class MultiServiceAggregatorTest {
    @Test void invokesThreeServicesConcurrently() throws Exception {
        CountDownLatch allStarted = new CountDownLatch(3);
        CountDownLatch release = new CountDownLatch(1);
        DownstreamClient blockedClient = id -> {
            allStarted.countDown();
            if (!release.await(5, TimeUnit.SECONDS)) throw new IllegalStateException("test release timed out");
            return id;
        };
        Map<String, DownstreamClient> clients = clients(blockedClient, blockedClient, blockedClient);
        try (MultiServiceAggregator aggregator = new MultiServiceAggregator(clients, Duration.ofSeconds(10), 1, new InMemoryRepository(), 3)) {
            CompletableFuture<AggregateResponse> response = CompletableFuture.supplyAsync(() -> aggregator.aggregate("r1"));
            assertTrue(allStarted.await(5, TimeUnit.SECONDS), "all downstream calls must start before any completes");
            release.countDown();
            AggregateResponse completed = response.get(5, TimeUnit.SECONDS);
            assertTrue(completed.complete());
            assertEquals(java.util.List.of("one", "two", "three"), new ArrayList<>(completed.results().keySet()));
        }
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

    @Test void rejectsConfigurationThatCannotStartAllCallsConcurrently() {
        Map<String, DownstreamClient> clients = clients(id -> "a", id -> "b", id -> "c");
        assertThrows(IllegalArgumentException.class,
                () -> new MultiServiceAggregator(clients, Duration.ofSeconds(1), 1, new InMemoryRepository(), 2));
    }

    private Map<String, DownstreamClient> clients(DownstreamClient one, DownstreamClient two, DownstreamClient three) {
        Map<String, DownstreamClient> clients = new LinkedHashMap<>(); clients.put("one", one); clients.put("two", two); clients.put("three", three); return clients;
    }
    private static String delayed(String value, long millis) throws InterruptedException { Thread.sleep(millis); return value; }
}
