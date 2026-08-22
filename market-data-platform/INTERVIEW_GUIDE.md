# 40–60 Minute Interview Guide — Market Data

## What a Strong Final Answer Contains

The final board should show A/B multicast ingestion, sequence tracking and repair, venue normalization, symbol-partitioned book builders, a durable normalized stream, fan-out tiers, slow-consumer isolation, recovery, and data-quality observability.

## Timed Plan

| Time      | Output                                                                             |
| --------- | ---------------------------------------------------------------------------------- |
| 0–5 min   | Clarify asset classes, feeds, consumers, depth, latency, entitlement, and recovery |
| 5–10 min  | Estimate packets/s, bytes/s, symbols, book memory, and fan-out                     |
| 10–20 min | Draw feed handlers → sequencer → normalizer → book shards → fan-out                |
| 20–32 min | Walk a packet and reconstruct one book                                             |
| 32–42 min | Deep dive on loss, A/B arbitration, retransmission, and snapshots                  |
| 42–50 min | Slow consumers, scaling, HA, and shard movement                                    |
| 50–60 min | Data quality, observability, security/entitlements, trade-offs, recap              |

## 1. Clarify Scope

Ask whether consumers need raw ticks, normalized events, L1/L2/L3 books, trades, or snapshots; whether feeds are multicast A/B; how many venues/symbols; whether packet loss or late data may block a channel; and whether clients are low-latency strategies or slower UI/analytics users.

Assume 10 venues, 5 million packets/s peak across the plant, 500 bytes average after envelope overhead, 1 million instruments, p99 plant processing under 1 ms for low-latency subscribers, and snapshot recovery under 30 seconds. Call out that market bursts—not daily averages—size the system.

## 2. Requirements

Functional: receive A/B feeds, validate/checksum and sequence, repair gaps, normalize schemas, reconstruct books, publish raw/normalized/book products, enforce entitlements, record data, and recover consumers/shards.

Non-functional: preserve per-channel order, minimize jitter, never block ingestion on a slow consumer, detect corruption quickly, scale by independent channels/symbols, and reproduce a historical book deterministically.

Consistency: consumers may choose a low-latency stream that flags gaps or a complete stream that pauses until repaired. Make the product contract explicit.

## 3. Capacity Sketch

- 5M packets/s × 500 bytes ≈ 2.5 GB/s ingress before replication and decoded object overhead.
- Do not create many heap objects per tick; use binary buffers/ring buffers and preallocation in production.
- Partition first by venue/channel because sequence order lives there; after normalization repartition by symbol for book ownership.
- Estimate book memory from open orders, not symbol count alone. Ten million live orders × roughly 64 compact bytes is already hundreds of MB before indexes.
- Tier storage: short replay cache, durable normalized log, compressed historical archive.

## 4. Data Contracts

Venue packet envelope:

```text
(venue, channel, sequence, receiveTimestamp, messageType, rawPayload)
```

Normalized event:

```text
(symbol, eventType, orderId, side, priceTicks, quantity, venueSequence, eventTime)
```

Book snapshot:

```text
(symbol, appliedSequence/offset, bids[price,qty], asks[price,qty], qualityFlags)
```

Represent prices as integer ticks and quantities as integers/decimals with explicit scale. Floating point belongs nowhere in book identity.

## 5. Architecture Walkthrough

1. Dedicated NIC/feed handler receives A and B copies and stamps receive time.
2. Channel sequencer discards duplicates and detects missing ranges.
3. A/B arbiter may fill a loss from the other leg; otherwise call TCP retransmission.
4. Normalizer maps venue fields to a canonical event while retaining the raw envelope.
5. Stream is repartitioned by symbol to exactly one book owner.
6. Book builder applies events in order and emits incremental updates and periodic snapshots.
7. Fan-out gateways serve separate products and isolate subscriber queues.
8. Normalized log and snapshots provide replay, analytics, and restart recovery.

## 6. Deep Dives

### Packet loss

For `seq > expected`, record the gap, buffer later packets within a bound, check the redundant feed, then request retransmission. Do not mutate the book with later events until the required prefix is complete. If the replay window is gone, load a venue snapshot and apply increments from its declared boundary.

For prolonged gaps choose deliberately: block only the affected channel/symbol set, mark output stale, and alert. Never silently synthesize missing order events.

### Book reconstruction

Maintain order ID → side/price/remaining quantity plus price → aggregate quantity/order queue. Apply add, modify/reduce, delete, execute, and replace semantics exactly as specified by each venue. Assert invariants: no negative quantity, legal transitions, monotonic sequence, and normally best bid below best ask.

### Slow consumers

Never let subscriber TCP pressure reach a book shard. Give each client/product a bounded queue. Strategies needing every event are disconnected and must replay; UI clients receive conflated latest snapshots; analytics consume the durable log independently.

## 7. HA, Scaling, and Recovery

- Deploy redundant receivers on separate hosts/switch paths.
- Book partitions have one active owner and warm standbys or fast replay from snapshot + log.
- Snapshot contains its exact input offset. Restore it, replay the tail, validate checksums, then mark ready.
- Move a symbol at a sequence barrier: freeze old owner output, transfer snapshot/offset, catch up, atomically change routing.
- Scale fan-out independently from ingestion and book building.

## 8. Observability, Quality, and Security

Track packets/s, kernel drops, A/B divergence, expected vs received sequence, gap size/age, replay latency, decode failures, crossed/locked books, event-to-publish latency, shard lag, queue depth, disconnects, and conflation count. Preserve receive, decode, apply, and publish timestamps for latency attribution.

Enforce client entitlements at fan-out, encrypt non-multicast links, audit subscription changes, protect vendor credentials, and prevent data leakage across tenants/regions.

## 9. Trade-Offs

- Waiting for repair gives correctness but increases latency; offer clear feed products rather than one compromise.
- L3 books answer order-level questions but cost much more memory than L2 aggregates.
- Normalization simplifies consumers but must retain venue-specific fields needed for accurate semantics.
- Kernel bypass and busy spinning reduce latency at the cost of cores and operational complexity.
- Frequent snapshots speed recovery but consume bandwidth/CPU; tune by replay rate and RTO.

## 10. Runnable Proof

Run `mvn test` and `mvn exec:java`. The demo proves ordered gap repair, normalization, book aggregation, and two slow-consumer policies. Be explicit that the map-based replay source substitutes for A/B multicast and a TCP replay service.
