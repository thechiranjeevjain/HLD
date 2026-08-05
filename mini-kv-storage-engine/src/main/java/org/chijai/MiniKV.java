package org.chijai;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.*;

/**
 * ============================================================
 * MiniKV — A First-Principles Key-Value Store (FORGING EDITION)
 * ============================================================
 *
 * GOAL:
 * -----
 * This file is not "production-ready".
 * It is intentionally minimal, explicit, and opinionated.
 *
 * The purpose is to FORCE understanding of:
 * - durability
 * - ordering
 * - failure modes
 * - concurrency
 * - trade-offs
 *
 * DESIGN PHILOSOPHY:
 * ------------------
 * 1. Correctness > performance
 * 2. Explicit > clever
 * 3. Failures are normal, not exceptional
 *
 * If something feels "manual" or "inelegant",
 * that is a FEATURE, not a bug.
 *
 * RUN:
 * ----
 *   javac MiniKV.java
 *   java MiniKV
 *
 * ============================================================
 */

/**
 * ============================================================
 * HOW THIS FILE SHOULD BE READ
 * ============================================================
 *
 * This file is NOT meant to be read top-to-bottom like a script.
 * It is meant to be understood as a system.
 *
 * Recommended reading order:
 *
 * 1. SYSTEM INVARIANTS — what must never break
 * 2. WRITE PATH — how durability is enforced
 * 3. READ PATH — how correctness is preserved
 * 4. FAILURE HANDLING — crashes, corruption, expiry
 * 5. TRADE-OFFS — what this system intentionally does NOT solve
 *
 * Treat every class and method as existing
 * only to protect one or more invariants.
 *
 * This is a teaching and reasoning artifact,
 * not a production library.
 */
public class MiniKV {

    /* ============================================================
     * CONFIGURATION
     * ============================================================
     *
     * Trade-off:
     * ----------
     * - Constants instead of config files keep cognitive load low.
     * - In real systems, these would be externalized.
     */
    static final String WAL_FILE = "wal.log";
    static final int CACHE_CAPACITY = 3;
    static final long CLEANUP_INTERVAL_MS = 2000;

    /* ============================================================
     * VALUE MODEL
     * ============================================================
     *
     * Why wrap value?
     * ----------------
     * - TTL introduces time as a dimension.
     * - Raw String would not carry expiry semantics.
     *
     * Trade-off:
     * ----------
     * - Storing expiry per value increases memory
     * - But simplifies cleanup and reads
     */
    static class Value {
        final String data;
        final long expiry; // epoch millis, -1 = immortal

        Value(String data, long ttlMs) {
            this.data = data;
            this.expiry = ttlMs <= 0
                    ? -1
                    : System.currentTimeMillis() + ttlMs;
        }

        boolean isExpired() {
            return expiry != -1 && System.currentTimeMillis() > expiry;
        }
    }

    /**
     * ============================================================
     * SYSTEM INVARIANTS (SINGLE SOURCE OF TRUTH)
     * ============================================================
     *
     * Invariant #1 — Durability
     * Once a write is acknowledged, it must survive process crash.
     *
     * Invariant #2 — Ordering
     * Writes must be replayed in the exact order they were accepted.
     *
     * Invariant #3 — Visibility & Correctness
     * Reads must never return deleted or expired data.
     *
     * Invariant #4 — Safety over Performance
     * The system may be slow, but it must not lie.
     *
     * Every major design decision in this file
     * (WAL ordering, locking, TTL strategy, cache behavior)
     * exists ONLY to protect these invariants.
     *
     * If a proposed optimization violates any invariant,
     * it is rejected by design.
     */

    /* ============================================================
     * KV STORE (CORE)
     * ============================================================
     */
    static class KVStore {

        private final Map<String, Value> store = new HashMap<>();
        private final ReadWriteLock lock = new ReentrantReadWriteLock();
        private final LRUCache cache = new LRUCache(CACHE_CAPACITY);
        private final WAL wal = new WAL(WAL_FILE);

        private final ScheduledExecutorService cleaner =
                Executors.newSingleThreadScheduledExecutor();

        KVStore() {
            recover();
            startCleanup();
        }

        /* ===================== WRITE PATH ===================== */

        void put(String key, String value, long ttlMs) {
            lock.writeLock().lock();
            try {
                wal.append("PUT|" + key + "|" + value + "|" + ttlMs);
                Value v = new Value(value, ttlMs);
                store.put(key, v);
                cache.put(key, v);
            } finally {
                lock.writeLock().unlock();
            }
        }

        /* ===================== READ PATH ===================== */

        String get(String key) {
            lock.readLock().lock();
            try {
                Value v = cache.get(key);
                if (v == null) v = store.get(key);
                if (v == null || v.isExpired()) return null;
                cache.put(key, v);
                return v.data;
            } finally {
                lock.readLock().unlock();
            }
        }

        /* ===================== DELETE ===================== */

        void delete(String key) {
            lock.writeLock().lock();
            try {
                wal.append("DEL|" + key);
                store.remove(key);
                cache.remove(key);
            } finally {
                lock.writeLock().unlock();
            }
        }

        /* ===================== RECOVERY ===================== */

        void recover() {
            List<String> records = wal.readAll();
            for (String r : records) {
                try {
                    String[] p = r.split("\\|");
                    if (p[0].equals("PUT")) {
                        store.put(p[1],
                                new Value(p[2], Long.parseLong(p[3])));
                    } else if (p[0].equals("DEL")) {
                        store.remove(p[1]);
                    }
                } catch (Exception e) {
                    System.err.println("Skipping corrupted WAL entry: " + r);
                }
            }
        }

        /* ===================== TTL CLEANUP ===================== */

        void startCleanup() {
            cleaner.scheduleAtFixedRate(() -> {
                lock.writeLock().lock();
                try {
                    Iterator<Map.Entry<String, Value>> it = store.entrySet().iterator();
                    while (it.hasNext()) {
                        var e = it.next();
                        if (e.getValue().isExpired()) {
                            it.remove();
                            cache.remove(e.getKey());
                        }
                    }
                } finally {
                    lock.writeLock().unlock();
                }
            }, CLEANUP_INTERVAL_MS, CLEANUP_INTERVAL_MS, TimeUnit.MILLISECONDS);
        }

        /* ===================== WAL COMPACTION ===================== */

        void compactWAL() {
            lock.writeLock().lock();
            try {
                wal.rewrite(store);
            } finally {
                lock.writeLock().unlock();
            }
        }

        void shutdown() {
            cleaner.shutdownNow();
        }
    }

    /* ============================================================
     * LRU CACHE
     * ============================================================
     */
    static class LRUCache {
        private final int capacity;
        private final LinkedHashMap<String, Value> map;

        LRUCache(int capacity) {
            this.capacity = capacity;
            this.map = new LinkedHashMap<>(capacity, 0.75f, true) {
                protected boolean removeEldestEntry(Map.Entry<String, Value> e) {
                    return size() > LRUCache.this.capacity;
                }
            };
        }

        Value get(String k) { return map.get(k); }
        void put(String k, Value v) { map.put(k, v); }
        void remove(String k) { map.remove(k); }
    }

    /* ============================================================
     * WRITE-AHEAD LOG (WAL)
     * ============================================================
     */
    static class WAL {
        private final Path path;

        WAL(String file) {
            this.path = Paths.get(file);
        }

        synchronized void append(String record) {
            try {
                Files.write(path,
                        (record + "\n").getBytes(StandardCharsets.UTF_8),
                        StandardOpenOption.CREATE,
                        StandardOpenOption.APPEND);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        List<String> readAll() {
            if (!Files.exists(path)) return List.of();
            try {
                return Files.readAllLines(path, StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        void rewrite(Map<String, Value> store) {
            try (BufferedWriter w = Files.newBufferedWriter(
                    path,
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING)) {

                for (var e : store.entrySet()) {
                    Value v = e.getValue();
                    long ttl = v.expiry == -1
                            ? -1
                            : v.expiry - System.currentTimeMillis();
                    w.write("PUT|" + e.getKey() + "|" + v.data + "|" + ttl);
                    w.newLine();
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /* ============================================================
     * DEMO / SMOKE TEST
     * ============================================================
     */
    public static void main(String[] args) throws Exception {
        KVStore kv = new KVStore();

        ExecutorService writers = Executors.newFixedThreadPool(3);

        writers.submit(() -> kv.put("a", "1", 5000));
        writers.submit(() -> kv.put("b", "2", 0));
        writers.submit(() -> kv.put("c", "3", 2000));

        writers.shutdown();
        writers.awaitTermination(2, TimeUnit.SECONDS);

        System.out.println("a=" + kv.get("a"));
        System.out.println("b=" + kv.get("b"));
        System.out.println("c=" + kv.get("c"));

        Thread.sleep(3000);
        System.out.println("c after expiry=" + kv.get("c"));

        kv.compactWAL();
        System.out.println("WAL compacted at " + Instant.now());
        kv.shutdown();
    }

    /**
     * ============================================================
     * TEACHING & INTERVIEW APPENDIX
     * ============================================================
     *
     * HOW TO EXPLAIN THIS SYSTEM (INTERVIEW MODE)
     * -------------------------------------------
     *
     * 1. Start with invariants, not implementation.
     *    - Durability
     *    - Ordering
     *    - Expiry correctness
     *
     * 2. Explain the write path clearly:
     *    - WAL append BEFORE memory mutation
     *    - Crash safety depends on this ordering
     *
     * 3. Explain TTL handling:
     *    - Lazy expiration on reads
     *    - Periodic background cleanup
     *    - Trade-off: simplicity over precision
     *
     * 4. Explain concurrency model:
     *    - Coarse-grained locking chosen deliberately
     *    - Atomicity across WAL + store + cache
     *
     * 5. State limitations calmly:
     *    - Single-node
     *    - No replication
     *    - Throughput sacrificed for correctness
     *
     * KEY FRAMING SENTENCE:
     * "This system is intentionally constrained to surface
     * failure modes clearly rather than hide them with abstractions."
     *
     * WHAT NOT TO SAY:
     * - "This is just a toy"
     * - "I would just use Redis"
     *
     * SENIOR SIGNAL:
     * - Agreeing with critiques
     * - Explaining trade-offs without defensiveness
     *
     * TEACHING MODE:
     * --------------
     * Always start with failure scenarios:
     * - Crash between WAL and memory
     * - Corrupted WAL entry
     * - Expired key visibility
     * - Concurrent writes
     *
     * Code comes last. Reasoning comes first.
     */

    /**
     * ============================================================
     * INTERVIEWER GRILLING — SELF-SIMULATION
     * ============================================================
     *
     * Q1: Why did you use a Write-Ahead Log instead of writing directly
     *     to the in-memory map?
     *
     * A:
     * Durability. If the process crashes after acknowledging a write
     * but before memory mutation, WAL replay guarantees recovery.
     *
     * ------------------------------------------------------------
     *
     * Q2: What happens if the system crashes after WAL append
     *     but before updating the HashMap?
     *
     * A:
     * On restart, the WAL is replayed and the write is applied.
     * This is why WAL ordering precedes memory mutation.
     *
     * ------------------------------------------------------------
     *
     * Q3: Why not use ConcurrentHashMap and avoid locking?
     *
     * A:
     * Because correctness spans multiple structures:
     * WAL, store, and cache must be updated atomically.
     * ConcurrentHashMap alone does not guarantee this.
     *
     * ------------------------------------------------------------
     *
     * Q4: Your TTL implementation is imprecise. Isn't that a bug?
     *
     * A:
     * It's a deliberate trade-off. Lazy expiry simplifies correctness.
     * Many production systems accept similar TTL drift.
     *
     * ------------------------------------------------------------
     *
     * Q5: This won't scale. How would you fix that?
     *
     * A:
     * First, segment the WAL and add checksums.
     * Second, reduce lock contention.
     * Third, introduce replication or sharding.
     *
     * ------------------------------------------------------------
     *
     * Q6: What is the weakest part of this design?
     *
     * A:
     * Single-node durability. A disk failure loses data.
     * Replication would be required for true fault tolerance.
     *
     * ------------------------------------------------------------
     *
     * Q7: What part of this system would you redesign first
     *     if given more time?
     *
     * A:
     * WAL segmentation and crash-safe compaction,
     * because replay time and corruption risk grow with size.
     *
     * ------------------------------------------------------------
     *
     * Q8: If I asked you to remove the cache entirely,
     *     would correctness change?
     *
     * A:
     * No. Only performance would degrade.
     * Cache is an optimization, not an invariant-holding component.
     *
     * ------------------------------------------------------------
     *
     * Q9: Where could subtle bugs hide in this code?
     *
     * A:
     * TTL expiry vs concurrent reads,
     * compaction timing,
     * and crash during WAL rewrite.
     *
     * ------------------------------------------------------------
     *
     * Q10: Why is this project valuable beyond backend roles?
     *
     * A:
     * It trains invariant-driven thinking,
     * explicit trade-offs,
     * and failure-first design —
     * skills transferable to architecture and consulting.
     *
     * ============================================================
     */

}

