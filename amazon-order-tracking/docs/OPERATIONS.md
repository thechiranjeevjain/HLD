# Operations and Failure Drills

## Duplicate storm

Replay the same `scripts/demo-events.ps1` request. First response is `ACCEPTED`; later responses are `DUPLICATE`. Alert if a carrier's duplicate ratio changes sharply—it often signals retry or acknowledgment trouble.

## Out-of-order scan

POST a `PACKED` event timestamped before the seeded `SHIPPED`. It appears at the correct point in history while current status remains `SHIPPED`. In production, compare event-time lateness histograms by carrier and tune watermarks without discarding late events.

## Missing updates

A scheduled scanner finds non-terminal shipments whose `status_time` exceeds carrier/service thresholds. It marks customer-facing freshness, opens an internal alert, and enqueues a rate-limited carrier API pull. Reconciliation appends recovered events through the same ingest contract. Nightly full reconciliation is the safety net.

## Poison event / schema drift

Strict carrier adapters reject unknown mandatory fields or invalid enum mappings to a DLQ containing payload reference, schema version, error, carrier, and receive time. Page on oldest-message age, not just count. Replay is permissioned, audited, rate-limited, and uses the original idempotency key after an adapter fix.

## Projection lag or outage

Continue durable ingest while storage permits; return accepted once the event is committed. Serve the last projection with an explicit timestamp. Shed nonessential support/raw reads first. After recovery, processors resume checkpoints and idempotently backfill.

## Cache stampede

Short TTL plus per-key single-flight means concurrent misses share one load. Add TTL jitter in Redis. If Redis fails, bypass it with read-store rate protection; cache failure must not become tracking failure.

## Authorization incident

At the edge, resolve order ownership from trusted identity claims—not request headers—and fail closed. Audit `order_id`, actor, role, purpose/case ID for support, timestamp, decision, and request correlation ID. Rate limit per account and investigate unusual cross-order access. The headers in this demo are intentionally only a visible local substitute.

## SLOs and alerts

- Availability: 99.95% monthly for tracking reads.
- Freshness: p99 accepted event visible within 30 seconds under healthy dependencies.
- Correctness: zero known cross-account reads; duplicate processing must not regress state.
- Alert on projection lag, oldest DLQ message, stale shipment ratio, carrier reject-rate change, cache hit collapse, and authorization-denial spikes.
