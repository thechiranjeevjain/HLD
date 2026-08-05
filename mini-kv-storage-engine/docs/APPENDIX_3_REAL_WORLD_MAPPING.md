# MiniKV — Real World Mapping

This project does not replace real systems.
It explains **why they look the way they do**.

---

## Databases (Postgres, MySQL, RocksDB)
- WAL for durability
- Crash recovery via replay
- Log compaction

---

## Caching Systems (Redis, Memcached)
- In-memory speed
- TTL expiration
- Eviction policies

Cache is optimization, not truth.

---

## Kafka / Streaming Systems
- Append-only logs
- Ordered writes
- Replayability

Kafka is essentially a distributed WAL.

---

## Payments & FinTech
- Never acknowledge unrecoverable state
- Ordering prevents double spends

Correctness > latency.

---

## Operating Systems & File Systems
- Journaling file systems
- Metadata logged before mutation

Same invariant: recover to consistency.

---

## Product Systems
- TTL for sessions and tokens
- Cleanup jobs prevent leaks
- Ordering prevents user confusion

---

## Universal Rule

> Never acknowledge something you cannot recover.
