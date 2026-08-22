# Production Readiness — Market Data Platform

Current target: **I1 — interview-ready**. The simulator proves deterministic gap repair, book mutation, and slow-consumer policy. Read the shared [readiness definitions](../docs/READINESS_LEVELS.md).

## Current Evidence

| Area                               | Status                | Evidence                                                                            |
| ---------------------------------- | --------------------- | ----------------------------------------------------------------------------------- |
| Sequence gap repair and duplicates | `VERIFIED_LOCAL`      | Missing packet is recovered before a later packet is emitted                        |
| Normalization boundary             | `VERIFIED_LOCAL`      | Venue packet becomes a canonical event                                              |
| Order-book reconstruction          | `VERIFIED_LOCAL`      | Adds, reductions, trades, deletes, and price aggregation tests                      |
| Slow-consumer isolation            | `VERIFIED_LOCAL`      | Conflate-latest and disconnect policies tested independently                        |
| Multicast A/B reception            | `DESIGNED_ONLY`       | Map-backed recovery substitutes for redundant network feeds                         |
| Venue feed certification           | `EXTERNAL_DEPENDENCY` | Requires feed specifications, credentials, capture/replay data, and vendor approval |

## Production Gaps

| Workstream     | Production target                                                   | Required proof                                                                     |
| -------------- | ------------------------------------------------------------------- | ---------------------------------------------------------------------------------- |
| Feed ingestion | Redundant multicast receivers, NIC/kernel tuning, packet timestamps | Packet-loss, A/B divergence, burst, kernel-drop, and capture/replay evidence       |
| Decoding       | Venue-certified binary decoders and schema/version handling         | Golden packet corpus, malformed packets, protocol upgrade, and compatibility tests |
| Recovery       | Redundant-feed arbitration, TCP replay, snapshot fallback           | Replay-window expiry, double-feed loss, and snapshot/catch-up drills               |
| Books          | Complete venue-specific L2/L3 semantics and invariant checks        | Historical deterministic reconstruction and venue-reference comparisons            |
| Scaling        | Channel then symbol partitioning with safe shard transfer           | Hot-symbol, rebalance-at-barrier, failover, and replay-time tests                  |
| Fan-out        | Real protocols, entitlements, independent backpressure tiers        | Slow-client soak, disconnect/replay, conflation, and entitlement tests             |
| Storage        | Replicated normalized log, snapshots, and compliant archive         | Restore-from-offset, corruption detection, retention, and capacity proof           |
| Operations     | Data-quality/latency dashboards and quarantine runbooks             | Crossed-book, stale-channel, replay-lag, and capacity game days                    |

## Keep Outside the 60-Minute Answer

Do not spend interview time on NIC models, every venue message type, or complete entitlement schemas. Deep-dive sequence gaps, A/B/retransmission, book ownership, snapshot-plus-tail recovery, and why a slow subscriber cannot block ingestion.

## Promotion Path

1. P0: define feed products, completeness/latency contracts, capacity, quality policy, and recovery SLO.
2. P1: ingest and decode one real redundant feed into a durable normalized log and certified book.
3. P2: run loss/burst/soak, recovery, shard failover, and fan-out backpressure exercises.
4. P3: complete entitlements, vendor certification, operational readiness, archive/compliance, and rollout controls.
