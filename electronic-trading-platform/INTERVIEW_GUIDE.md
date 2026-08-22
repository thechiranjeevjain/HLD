# 40–60 Minute Interview Guide — End-to-End Electronic Trading

## What a Strong Final Answer Contains

The answer should connect client/algo, gateway, OMS, pre-trade risk, market data, smart routing, venue connectivity, executions, positions/P&L, event-driven recovery, and observability. Keep hot synchronous decisions separate from durable asynchronous projections.

## Timed Plan

| Time      | Output                                                                              |
| --------- | ----------------------------------------------------------------------------------- |
| 0–5 min   | Clarify users, products, order types, venues, latency, consistency, and regulations |
| 5–10 min  | Estimate orders/s, market-data rate, open orders, events/day, and SLOs              |
| 10–20 min | Draw the complete component and data-flow diagram                                   |
| 20–30 min | Walk a successful order and identify authoritative state at each step               |
| 30–40 min | Deep dive on risk reservations, OMS idempotency, and execution ordering             |
| 40–50 min | Uncertain outcomes, HA, replay, DR, and kill switches                               |
| 50–60 min | Observability, security, trade-offs, bottlenecks, recap                             |

## 1. Clarify Scope

Ask: institutional or retail; single region or global; supported order types/assets; number of clients/venues; target gateway/risk latency; whether the design includes matching; source of positions and limits; market-data products; recovery point/time; and regulatory retention.

Assume 100k order actions/s peak, 10x market-data messages, one million open orders, p99 internal accept/reject under 2 ms excluding venue WAN, no acknowledged order loss, and one-hour regional DR. Adjust when the interviewer gives numbers.

## 2. Requirements and Invariants

Functional: authenticate sessions, validate/idempotently accept orders, price them with fresh data, reserve risk synchronously, maintain OMS lifecycle, route to venues, process executions/cancels, update positions/P&L, recover, and give operators kill switches and audit.

Non-functional: deterministic per-account/order state, low latency and jitter, high availability, horizontal scale, bounded recovery, observability, security, and regulatory audit.

Core invariants:

- One client order ID maps to one logical intent.
- Nothing is sent before a successful risk reservation.
- Unknown venue outcomes retain capital until reconciled.
- Executions are immutable facts and drive positions.
- Every state transition is replayable and attributable.

## 3. Capacity Sketch

- 100k actions/s × perhaps 1 KB of command/events gives 100 MB/s before replicas/indexes.
- Market data is a separate, much larger pipeline; risk consumes compact latest marks/books, not the raw global feed.
- Partition order flow by account/client to localize risk state; partition venue connectivity by session; partition positions by account/instrument.
- Storage: immutable order/execution journal for audit and replay, OMS query store for current lifecycle, snapshots for hot-state recovery, archive for long retention.

## 4. External and Internal Contracts

Client command:

```text
NewOrder(clientOrderId, account, instrument, side, quantity, limitPrice, tif)
Cancel(clientCancelId, originalClientOrderId)
```

Synchronous result means accepted for processing or rejected by gateway/risk—not necessarily accepted by the venue.

Internal events include `OrderReceived`, `RiskReserved`, `VenueSent`, `VenueAck`, `Fill`, `CancelAck`, `OrderUnknown`, `RiskReleased`, and `PositionUpdated`. Carry client, internal, venue, and trace identifiers.

## 5. Architecture and Successful Flow

1. Gateway authenticates the client/session, validates schema and rate limits, and deduplicates the client order ID.
2. OMS/journal creates the durable logical order and routes it to the owning partition.
3. Risk reads in-memory limits, positions, open-order reservations, and a fresh market mark; it atomically reserves capacity.
4. Smart router selects venue using price, liquidity, fees, latency, and eligibility.
5. Connectivity owns the venue session, sequence, throttle, and wire protocol.
6. Venue reports flow through connectivity/drop copy to OMS.
7. Immutable fills update positions, P&L, risk usage, client reports, and downstream clearing asynchronously.
8. Metrics/logs/traces correlate the lifecycle without placing blocking telemetry on the hot path.

Authoritative sources: journal for order transitions, venue/drop-copy for execution truth, risk owner for current reservations, and position ledger/projection for positions. Avoid two databases independently claiming the same truth.

## 6. Deep Dives

### Risk hot path

One owner/event loop per account or risk group keeps limit check + reservation atomic without a remote database. Versioned limit snapshots arrive through the control plane. Snapshot and replay rebuild positions/open-order reservations before readiness. Fail closed on stale market data, missing limit state, split ownership, or kill switch.

### Idempotency and ordering

Gateway deduplicates by client/session/clientOrderId and returns the original result. OMS state transitions tolerate execution-before-ack and duplicates because networks are not globally ordered. Consumers dedupe immutable events by event/venue execution ID.

### Uncertain venue outcome

A socket can fail after send but before ack. Mark `UNKNOWN`, retain the risk reservation, query venue/drop copy, and prevent blind resend. Client status exposes uncertainty rather than inventing success or rejection.

## 7. HA, Recovery, and DR

- Active/active across account and session partitions; single fenced owner inside each partition.
- Replicate the command/event log across availability zones.
- Periodic snapshots include the precise log offset and configuration version.
- On restart: restore snapshot, replay tail, reconcile venue sessions/unknown orders, validate positions, then advertise readiness.
- Regional DR may use asynchronous replication with a stated RPO; reopening venue sessions requires operational coordination and reconciliation.
- Kill switches exist by global, desk, account, symbol, and venue scope with audited RBAC.

## 8. Observability and Security

Metrics: order/risk/route/venue latency histograms; rejects by reason; open/unknown order count and age; risk utilization; market-data age; session gaps; execution/position lag; journal replication/replay lag; queue depth; kill-switch changes. Use order-level tracing sparingly or sampled, with always-on correlation IDs and immutable audit.

Use mTLS/session credentials, secrets management, RBAC/ABAC, maker-checker approval for limits/kill switches, encryption, tamper-evident logs, clock sync, data minimization, and network segmentation.

## 9. Trade-Offs

- A database on every risk check is simpler but cannot meet strict tail latency or survive DB jitter; in-memory ownership requires careful replay.
- Synchronous durability protects orders but adds latency; define the acknowledgement boundary precisely.
- One giant service eases local transactions but couples scaling/failures; service boundaries should follow ownership and latency, not nouns alone.
- Eventual positions are scalable, but real-time reservations must cover their lag.
- Smart routing quality and model complexity must never obscure deterministic safety checks.

## 10. Runnable Proof

Run `mvn test` and `mvn exec:java`. The vertical slice proves authentication, stale-data/risk gating, OMS dedupe, uncertain outcome, execution-driven positions, metrics, and journal replay. It is intentionally one JVM; explain how each class becomes a partitioned service without claiming the demo has production distributed guarantees.
