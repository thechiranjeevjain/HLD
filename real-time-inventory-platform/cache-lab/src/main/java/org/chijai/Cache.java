package org.chijai;




/*
────────────────────────────────────────────────────────
1. FILE HEADER — PHILOSOPHY + GOAL
────────────────────────────────────────────────────────

System:
A single-process, in-memory Cache with LRU eviction and TTL expiration.

Mental shift it trains:
Separating optimization from truth.

Spine sentence (core lesson):
A cache may be wrong, but it must never decide what is right.

Metaphor (used once only):
A cache is like a whiteboard next to the real ledger — useful, fast, erasable, and never authoritative.
*/

/*
────────────────────────────────────────────────────────
2. HOW TO READ THIS FILE
────────────────────────────────────────────────────────

Read this file top-down, once slowly.

1) Read invariants first. They are the law.
2) Read write-path and read-path comments BEFORE code.
3) Treat every synchronized block as a correctness boundary.
4) Assume crashes can occur between any two lines.
5) The demo intentionally shows the cache being "wrong".

This is not a utility.
This is a judgment-training artifact.
*/

/*
────────────────────────────────────────────────────────
3. SYSTEM INVARIANTS (SINGLE SOURCE OF TRUTH)
────────────────────────────────────────────────────────

INVARIANTS (WHAT IS GUARANTEED):

I1. Cache NEVER defines correctness.
    Returned values may be stale or missing.
    Callers must tolerate this.

I2. Bounded memory usage.
    Entry count is capped and enforced via eviction.

I3. Bounded staleness.
    Any entry older than TTL is invalid and treated as a miss.

I4. Serialized mutation.
    All reads and writes are synchronized.
    No concurrent mutation exists.

I5. Internal structural consistency.
    Map and LRU list never disagree.

I6. Monotonic time for TTL.
    Uses System.nanoTime() to avoid wall-clock anomalies.

I7. Observability is best-effort, not authoritative.
    Metrics (hits/misses/evictions) may be approximate.
    Metrics MUST NOT influence correctness decisions.

NON-INVARIANTS (WHAT IS EXPLICITLY NOT GUARANTEED):

N1. Freshness.
    Cache may return stale data within TTL.

N2. Durability.
    Entire cache is lost on process crash.

N3. Fairness.
    Hot keys dominate access patterns.

N4. Idempotency.
    Repeated writes overwrite silently.

CONCURRENCY BOUNDARY:

- Entire cache protected by a single intrinsic lock.
- Reads and writes do NOT run concurrently.
- Throughput is sacrificed to preserve invariants I2 and I5.

CONSISTENCY / ORDERING:

- LRU order is updated only after successful read/write.
- Failure to update order may cause premature eviction.
- Premature eviction is acceptable; corruption is not.

TIME ASSUMPTIONS:

- TTL measured using monotonic time.
- Wall clock changes must not affect correctness.

Every invariant above is defended by code.
*/

/*
────────────────────────────────────────────────────────
4. CORE IMPLEMENTATION
────────────────────────────────────────────────────────
*/

import java.util.HashMap;
import java.util.Map;

public class Cache {

    /*
     * DATA MODEL
     *
     * Explicit entry structure instead of LinkedHashMap.
     * This exposes ordering, eviction, and failure reasoning.
     */
    private static final class Entry<K, V> {
        final K key;
        V value;
        long writeTimeNanos;
        Entry<K, V> prev;
        Entry<K, V> next;

        Entry(K key, V value, long writeTimeNanos) {
            this.key = key;
            this.value = value;
            this.writeTimeNanos = writeTimeNanos;
        }
    }

    /*
     * CORE ENGINE
     */
    public static final class LruTtlCache<K, V> {

        private final int maxEntries;
        private final long ttlNanos;

        private final Map<K, Entry<K, V>> map = new HashMap<>();

        private Entry<K, V> head; // MRU
        private Entry<K, V> tail; // LRU

        // Observability only — never correctness
        private long hits;
        private long misses;
        private long evictions;
        private long expirations;

        public LruTtlCache(int maxEntries, long ttlMillis) {
            if (maxEntries <= 0) throw new IllegalArgumentException();
            if (ttlMillis <= 0) throw new IllegalArgumentException();

            this.maxEntries = maxEntries;
            this.ttlNanos = ttlMillis * 1_000_000L;
        }

        /*
         * WRITE / MUTATION PATH
         *
         * Order:
         * 1) Capture time
         * 2) Lookup existing entry
         * 3) Update or insert
         * 4) Fix LRU position
         * 5) Enforce capacity
         *
         * Why this order:
         * - Evicting BEFORE insert risks evicting a hot entry.
         * - Inserting BEFORE linking risks internal inconsistency.
         *
         * Crash reasoning:
         * - Crash mid-write loses entire cache (acceptable).
         * - Partial structure mutation is impossible due to lock.
         *
         * Idempotency:
         * - NOT idempotent. Last write wins.
         */
        public synchronized void put(K key, V value) {
            long now = System.nanoTime();

            Entry<K, V> e = map.get(key);
            if (e != null) {
                e.value = value;
                e.writeTimeNanos = now;
                moveToHead(e);
                return;
            }

            Entry<K, V> ne = new Entry<>(key, value, now);
            map.put(key, ne);
            addToHead(ne);

            if (map.size() > maxEntries) {
                evictTail();
            }
        }

        /*
         * READ / OBSERVATION PATH
         *
         * Cache hit does NOT imply correctness.
         * Cache miss does NOT imply absence in reality.
         */
        public synchronized V get(K key) {
            Entry<K, V> e = map.get(key);
            if (e == null) {
                misses++;
                return null;
            }

            long now = System.nanoTime();
            if (now - e.writeTimeNanos > ttlNanos) {
                removeEntry(e);
                map.remove(key);
                expirations++;
                misses++;
                return null;
            }

            hits++;
            moveToHead(e);
            return e.value;
        }

        /*
         * EVICTION & CLEANUP
         *
         * Eviction is a strategy, not a failure.
         * Expiration is bounded dishonesty.
         */
        private void evictTail() {
            if (tail == null) return;
            Entry<K, V> victim = tail;
            removeEntry(victim);
            map.remove(victim.key);
            evictions++;
        }

        private void moveToHead(Entry<K, V> e) {
            if (e == head) return;
            removeEntry(e);
            addToHead(e);
        }

        private void addToHead(Entry<K, V> e) {
            e.prev = null;
            e.next = head;
            if (head != null) head.prev = e;
            head = e;
            if (tail == null) tail = e;
        }

        private void removeEntry(Entry<K, V> e) {
            if (e.prev != null) e.prev.next = e.next;
            else head = e.next;

            if (e.next != null) e.next.prev = e.prev;
            else tail = e.prev;

            e.prev = null;
            e.next = null;
        }

        /*
         * Observability only.
         * NEVER use these counters for decisions.
         */
        public synchronized void dumpStats() {
            System.out.println(
                    "hits=" + hits +
                            ", misses=" + misses +
                            ", evictions=" + evictions +
                            ", expirations=" + expirations +
                            ", size=" + map.size()
            );
        }
    }

    /*
     * 6. DEMO / SMOKE TEST
     *
     * Demonstrates:
     * - Hit
     * - TTL expiration
     * - LRU eviction
     * - Cache being wrong without breaking the system
     */
    public static void main(String[] args) throws Exception {
        LruTtlCache<String, String> cache = new LruTtlCache<>(2, 500);

        System.out.println("PUT A=1");
        cache.put("A", "1");

        System.out.println("GET A -> " + cache.get("A"));

        Thread.sleep(600);

        System.out.println("GET A after TTL -> " + cache.get("A"));

        System.out.println("PUT B=2");
        cache.put("B", "2");

        System.out.println("PUT C=3");
        cache.put("C", "3");

        System.out.println("PUT D=4 (evicts B)");
        cache.put("D", "4");

        System.out.println("GET B -> " + cache.get("B"));
        System.out.println("GET C -> " + cache.get("C"));
        System.out.println("GET D -> " + cache.get("D"));

        cache.dumpStats();

        System.out.println("Cache lied. System stayed correct.");
    }
}

/*
────────────────────────────────────────────────────────
7. KNOWLEDGE INDEX (CONTROLLED COMPRESSION)
────────────────────────────────────────────────────────

MENTAL MODELS:
- Cache is a hint, not a fact.
- TTL is bounded dishonesty.
- Eviction is pressure relief.

FAILURE TIMELINES:
- Process crash → full cache loss → acceptable.
- Wall clock jump → avoided via nanoTime.
- Cache hit with stale data → acceptable by design.

NEGATIVE EXAMPLE (INTENTIONALLY NOT IMPLEMENTED):
- Treating cache hit as authoritative breaks:
  balances, permissions, quotas, feature flags.

TRADE-OFFS:
- Single lock:
  + Simple invariants, easy failure reasoning
  - Throughput ceiling

BACKPRESSURE VS FAIRNESS:
- Cache sheds load via eviction.
- No fairness guarantees by design.

OS / RUNTIME MAPPING:
- Heap → cache storage
- Monitor → correctness boundary
- nanoTime → monotonic TTL

REAL-WORLD MAPPING:
- DB buffer cache
- Redis without persistence
- Payment systems (never trust cache for truth)

INTERVIEW ANCHORS:
- "Cache may lie, bounded by TTL."
- "Eviction protects memory, not correctness."

WEAKEST POINT:
- Single lock scalability.

FIRST PRODUCTION CHANGE:
- Segmented locking AFTER invariants are proven.
*/
