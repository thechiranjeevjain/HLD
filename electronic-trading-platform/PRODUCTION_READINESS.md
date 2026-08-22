# Production Readiness — End-to-End Electronic Trading Platform

Current target: **I1 — interview-ready**. The single-JVM vertical slice proves lifecycle integration; it does not claim distributed-service guarantees. Read the shared [readiness definitions](../docs/READINESS_LEVELS.md).

## Current Evidence

| Area                                          | Status                | Evidence                                                      |
| --------------------------------------------- | --------------------- | ------------------------------------------------------------- |
| Gateway → risk → OMS → venue → positions flow | `VERIFIED_LOCAL`      | End-to-end happy-path test and CLI demo                       |
| Risk rejection before venue send              | `VERIFIED_LOCAL`      | Connectivity send count remains zero on reject                |
| OMS idempotency                               | `VERIFIED_LOCAL`      | Duplicate client order returns original state                 |
| Uncertain venue result                        | `VERIFIED_LOCAL`      | Reservation retained until reconciliation                     |
| Event replay                                  | `VERIFIED_LOCAL`      | OMS and positions rebuilt from ordered local journal          |
| Distributed deployment and durable messaging  | `DESIGNED_ONLY`       | Classes define ownership boundaries inside one process        |
| Real venue/feed/clearing integration          | `EXTERNAL_DEPENDENCY` | Owned by external systems and the HLD 26–28 production tracks |

## Ownership: Integrate, Do Not Duplicate

| Concern                                              | Deep owner | HLD 29 responsibility                                    |
| ---------------------------------------------------- | ---------- | -------------------------------------------------------- |
| Pre-trade checks/limits/recovery                     | HLD 26     | Call the risk contract and honor reservations/rejections |
| Venue protocol/session correctness                   | HLD 27     | Route canonical intents and consume normalized reports   |
| Feed sequencing/books                                | HLD 28     | Consume fresh marks/books and react to quality flags     |
| OMS, end-to-end lifecycle, positions, reconciliation | HLD 29     | Own integration state and cross-service invariants       |

## Production Gaps

| Workstream               | Production target                                                     | Required proof                                                                  |
| ------------------------ | --------------------------------------------------------------------- | ------------------------------------------------------------------------------- |
| Service boundaries       | Partitioned gateway, OMS, risk, connectivity, executions, positions   | Contract, ordering, duplicate, timeout, and compatibility tests                 |
| Source of truth          | Durable order/execution journal plus OMS query projections            | Crash consistency, outbox/log publication, replay, and reconciliation proof     |
| Lifecycle                | Complete new/replace/cancel/partial-fill/bust/correct states          | Out-of-order and duplicate venue event corpus                                   |
| Risk/market/connectivity | Production adapters owned by HLD 26–28                                | Integrated failure and stale-input tests without reimplementing subsystems here |
| Positions/P&L            | Authoritative ledgers, intraday projections, reconciliation           | Late/bust/correct events, replay, close-of-day, and ledger comparison           |
| Security/compliance      | Client/session identity, authorization, surveillance/audit seams      | Threat model, penetration review, retention and access review                   |
| HA/DR                    | Fenced partition owners, multi-AZ durability, regional recovery       | Load, failover, replay, split-brain, and measured RPO/RTO exercises             |
| Operations               | Whole-order tracing, business/SLO dashboards, kill switches, runbooks | Game days spanning several services and controlled rollback                     |

## Keep Outside the 60-Minute Answer

Do not repeat the full internals of risk, FIX/OUCH, or market-data books. Draw those as bounded services, explain their contract and one failure interaction, and use the saved time for end-to-end ownership, acknowledgement semantics, uncertain outcomes, executions, positions, and recovery.

## Promotion Path

1. P0: define authoritative data, cross-service contracts, acknowledgement boundary, lifecycle state machine, SLOs, and threat/failure models.
2. P1: deploy durable OMS/journal and integrate representative HLD 26–28 adapters.
3. P2: run end-to-end load, duplicate/out-of-order, multi-service failure, replay, and regional-DR exercises.
4. P3: complete compliance, operations, clearing/reconciliation, deployment safety, and controlled rollout.
