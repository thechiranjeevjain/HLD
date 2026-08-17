# Mini KV Storage Engine Interview Guide

## Two-Minute Pitch

This project implements the core mechanics behind a storage engine: write-ahead logging, in-memory indexing, recovery, TTL expiry, cache optimization, and compaction. It is intentionally single-node so the durability and ordering invariants are easy to explain.

## What To Emphasize

- A write is acknowledged only after the WAL append succeeds.
- Recovery replays WAL records in accepted order.
- Deletes are represented as log records before removing memory state.
- TTL is enforced on reads and by cleanup.
- Cache can improve reads but cannot define correctness.

## Tradeoffs

| Decision | Benefit | Cost |
| --- | --- | --- |
| Single WAL file | Simple durability story | Replay time grows without segmentation |
| Coarse write lock | Atomic WAL/store/cache updates | Lower write concurrency |
| In-memory map | Fast reads | Dataset limited by heap |
| Simple compaction | Reduces stale WAL entries | Crash-safe compaction needs more work |

## FAQ

Q: What happens if the process crashes after WAL append but before memory update?
A: Replay applies the record on restart, which is why WAL comes first.

Q: Why not use `ConcurrentHashMap` alone?
A: Correctness spans WAL, store, and cache. A concurrent map does not make those updates atomic together.

Q: What would you add next?
A: segmented WAL, checksums, snapshots, crash-safe compaction, bloom filters, and replication.
