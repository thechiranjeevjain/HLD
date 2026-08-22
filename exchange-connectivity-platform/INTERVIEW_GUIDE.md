# 40–60 Minute Interview Guide — Exchange Connectivity

## What a Strong Final Answer Contains

By the end, the board should show requirements and scale, a session-affine architecture, the order state machine, sequence recovery, throttling, HA/fencing, uncertain-outcome reconciliation, and observability. Do not spend the interview designing an exchange matching engine; this system connects an OMS to venues.

## Timed Plan

| Time      | Output                                                                                            |
| --------- | ------------------------------------------------------------------------------------------------- |
| 0–5 min   | Clarify venues, protocols, order types, latency, delivery semantics, and disaster-recovery target |
| 5–10 min  | Estimate sessions, messages/second, bandwidth, storage, and SLOs                                  |
| 10–20 min | Draw OMS → router → protocol/session engines → venues, plus journal, lease, and drop copy         |
| 20–32 min | Walk one new order and its state machine                                                          |
| 32–42 min | Deep dive on sequences, resend, dedupe, and disconnect-after-write                                |
| 42–50 min | HA, session ownership, fencing, throttling, and recovery                                          |
| 50–60 min | Observability, security, trade-offs, bottlenecks, and recap                                       |

## 1. Clarify Scope

Ask:

- FIX, OUCH, or both? Persistent TCP sessions with venue-assigned credentials?
- Must cancels get priority over new orders during throttling?
- Is the SLO internal gateway latency or end-to-end venue latency?
- Is active/standby acceptable per session, even if the platform is active/active across sessions?
- Are drop-copy and order-status-query channels available for reconciliation?
- How long must raw messages and business audit data be retained?

State assumptions: 20 venues, 200 logical sessions, 100k outbound messages/s peak per region, 2x bursts, p99 internal processing under 500 μs, no order loss, and RTO under 30 seconds for one session. Exact numbers matter less than using them to justify partitions and retention.

## 2. Requirements

Functional: accept canonical order/cancel requests, route to a venue, encode FIX/OUCH, maintain sessions and heartbeats, persist sequence state, recover gaps, throttle, deduplicate retries, fail over, reconcile uncertain outcomes, and emit normalized execution reports.

Non-functional: strict order within one session, high availability, bounded latency, auditability, secure credentials, horizontal scale across sessions, and no silent loss or duplication.

Delivery contract: “exactly once” cannot be promised across a socket failure. Promise idempotent intent handling plus at-least-once recovery and reconciliation to venue truth.

## 3. Capacity Sketch

- 100k messages/s × roughly 500 bytes encoded/logged ≈ 50 MB/s before replication.
- With 3x replication and indexes, provision well above 150 MB/s sustained journal write capacity.
- Session ordering is the primary partition key: `venue + sessionId`.
- A few hot sessions can dominate load, so one event loop may host several quiet sessions but a hot session may need its own core.
- Retain raw wire data for replay/audit according to regulation; compact operational sequence snapshots separately.

## 4. APIs and State

Internal command:

```text
SendOrder(clientOrderId, account, venue, symbol, side, qty, price, deadline)
CancelOrder(clientCancelId, originalClientOrderId, venue)
```

Normalized result:

```text
ExecutionReport(clientOrderId, venueOrderId, state, fills, venueSequence, eventTime)
```

Order states:

```text
RECEIVED → JOURNALED → SENT → ACKNOWLEDGED → PARTIAL_FILL → FILLED
                            ↘ REJECTED
                            ↘ UNKNOWN → reconciled to accepted/rejected
```

Persist session ID, protocol, next sender/target sequences, fencing epoch, raw payload hash, client ID, venue ID, and state transition. Index by client ID and venue ID.

## 5. Architecture Walkthrough

1. OMS sends a canonical command with a globally unique client order ID.
2. Venue router selects the session using policy and account/venue permissions.
3. The session owner checks its fencing lease and venue throttle.
4. It journals the outbound intent and assigned sequence before socket write.
5. Protocol codec writes FIX tags or OUCH bytes.
6. Inbound reports are sequence-checked, durably recorded, normalized, and delivered to OMS.
7. Drop copy independently confirms venue truth and helps reconcile gaps/unknowns.

Keep a single writer per session. Scale the platform by assigning different sessions to different owners, not by letting several processes concurrently mutate one sequence stream.

## 6. Deep Dives

### Sequence recovery

For inbound `seq > expected`, stop applying later business messages, buffer a bounded window, send FIX Resend Request or use the venue replay channel, process gap fills/poss-dup messages idempotently, and resume at the first contiguous sequence. For `seq < expected`, validate `PossDup`/business key and suppress the duplicate.

On outbound recovery, restore the last durably assigned sequence. Venue rules decide whether to reset, replay, or gap-fill administrative/application messages. Never invent a reset without bilateral agreement.

### Uncertain outcome

If the process wrote bytes and lost the socket before acknowledgement, retrying the order can double-execute. Mark it `UNKNOWN`, retain risk, stop automated blind resend, then reconcile through order-status request, drop copy, executions, or operator workflow. Alert on age and expose the state to clients.

### Throttling

Use per-venue/session token buckets. Reserve capacity for cancels and mass-cancel. Queue only within a deadline and bounded memory; otherwise reject before wire send. Persist only commands that the recovery policy knows how to handle.

## 7. HA and Recovery

- Run active/standby per session across failure domains.
- Store ownership in a strongly consistent lease service with monotonically increasing fencing tokens.
- Every send verifies the current token; the old primary must fail closed after partition.
- Replicate the journal synchronously enough to meet the no-loss contract.
- Standby restores snapshot + tail, logs on with the correct sequence, reconciles unknowns, then advertises readiness.
- Active/active exists at platform level because different session partitions are active on different nodes.

## 8. Observability and Security

Track send/ack latency, session state, heartbeats, sequence gaps, replay ranges, throttle depth, unknown-order count/age, journal lag, lease changes, and drop-copy divergence. Use correlation IDs across client, OMS, gateway, and venue IDs.

Store venue credentials in a secrets manager, use TLS/mTLS where supported, restrict operator actions with RBAC, record tamper-evident audit logs, synchronize clocks, and separate management traffic from the data path.

## 9. Trade-Offs to Say Explicitly

- Single writer limits one session to one core but makes ordering and recovery understandable.
- Synchronous journal replication adds latency but closes the loss window.
- Queueing through throttle improves acceptance but increases stale-order risk; bounded deadline-aware queues are safer.
- FIX is flexible and human-readable; OUCH is narrower and faster. Normalize only at boundaries without erasing venue-specific semantics.
- Drop copy is independent evidence, not automatically authoritative for every transient ordering race.

## 10. Runnable Proof

`ExchangeConnectivityPlatform` maps these claims to executable behavior. Run `mvn test` and `mvn exec:java`. In an interview, show the `UNKNOWN` result, standby sequence restoration, stale-primary fencing, and resend request; then clearly label the replicated journal and real protocol transport as production substitutions.
