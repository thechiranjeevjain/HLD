# Interview Guide

## Two-minute answer

“I first separate the immutable truth from the customer view. Every carrier webhook or polled update is authenticated, normalized, deduplicated by idempotency key and payload hash, then appended to an event store. A stream processor partitioned by shipment ID handles normal reordering with watermarks and deterministically projects a versioned `ShipmentState` plus timeline. It uses conditional writes, so retries are harmless.

Customers read by order ID, so I maintain an order-to-shipments index and a denormalized read model. A short-TTL Redis cache absorbs refresh traffic, with request coalescing on misses. Eventual consistency is acceptable and the UI exposes last-updated time. Missing updates trigger stale-shipment alerts and carrier reconciliation. Poison events go to a DLQ with replay.

Authorization happens before order data is returned: buyer ownership or a support role, with support access audited. Raw payloads have a shorter retention window, but normalized events remain immutable for audit and backfill.”

## Requirements to clarify aloud

- Are partial deliveries represented per package? Yes; aggregate `DELIVERED` only when every package is delivered.
- Freshness SLO? Target p99 carrier-event-to-read-model under 30 seconds when carriers are healthy.
- History retention and geographic residency? Product/legal decision; do not casually TTL normalized audit events.
- Who may see address/location detail? Apply field-level policy and avoid storing plaintext address in tracking services.
- Are customer notifications in scope? Consume state transitions downstream; do not couple them to webhook acknowledgment.

## Tradeoffs

| Decision | Benefit | Cost / mitigation |
| --- | --- | --- |
| Immutable events + projection | audit, replay, bug repair | storage and backfill complexity |
| Eventual consistency | scalable decoupled ingestion | show freshness; support raw view |
| Shipment write partition | ordered independent processing | order reads need fanout index |
| Short cache TTL | absorbs refresh bursts | stale seconds; invalidate on projection |
| At-least-once processing | practical recovery | idempotent consumer and CAS required |
| Carrier-specific adapters | stable internal schema | adapter ownership/schema metrics |

## Follow-up answers

**Why not update the order row in the webhook?** Carrier retries and slow projections would couple acknowledgment to read work, while multi-package concurrency creates contention. Append quickly and project asynchronously.

**Exactly once?** I avoid claiming it. Transport is at-least-once; dedupe keys, deterministic projection, and conditional writes produce effectively-once business outcomes.

**What if delivered arrives before shipped?** Preserve both. Projection uses event time, so the newest valid event controls state. When shipped arrives late, history becomes complete without regressing delivered.

**What about malicious webhooks?** Per-carrier mTLS or HMAC, replay-window validation, IP policy where useful, secrets rotation, strict schemas, payload limits, quotas, and encrypted raw payload storage.

**What would you monitor?** Ingest acceptance/duplicate/reject rate by carrier, event-to-projection lag p50/p95/p99, partition skew, stale shipments, DLQ depth/age, CAS conflicts, cache hit ratio, authorization denials, and support access volume.

## What this implementation proves

Run the tests and demo to show multi-package reads, owner isolation, support access, duplicate suppression, and an out-of-order event that enriches history without rolling state backward. Be explicit that external infrastructure and scale numbers are design evidence, not locally load-tested claims.
