# Cache Lab Interview Guide

## Two-Minute Pitch

This lab implements a bounded in-memory cache with LRU eviction and TTL expiry. The key interview lesson is that a cache is an optimization, not the source of truth. Correct systems must tolerate cache misses, eviction, and bounded staleness.

## What To Emphasize

- Capacity is enforced through LRU eviction.
- TTL is measured with `System.nanoTime()` so wall-clock changes do not affect expiry.
- A cache hit may still be logically stale in broader systems.
- The single lock keeps map and list structure consistent.
- Metrics are observability only; they do not decide correctness.

## Tradeoffs

| Decision | Benefit | Cost |
| --- | --- | --- |
| Manual list plus map | Makes LRU mechanics visible | More code than `LinkedHashMap` |
| Single lock | Simple structural correctness | Lower throughput under contention |
| TTL on read | Cheap cleanup behavior | Expired items may occupy space until read or eviction |
| In-memory only | Fast and simple | Lost on process restart |

## FAQ

Q: Why is a cache not authoritative?
A: It can be stale, evicted, or lost. The backing system must remain the source of truth.

Q: Why use monotonic time?
A: TTL should not break if the wall clock jumps forward or backward.

Q: What would you add next?
A: segmented locking, background cleanup, max memory by bytes, cache-aside loader support, and eviction metrics export.
