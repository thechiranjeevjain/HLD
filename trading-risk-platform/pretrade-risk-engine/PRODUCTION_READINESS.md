# Production Readiness — Pre-Trade Risk Platform

Current target: **I1 — interview-ready**. The local module proves key risk invariants; it is not presented as a deployable bank risk platform. Read the shared [readiness definitions](../../docs/READINESS_LEVELS.md) before interpreting this matrix.

## Current Evidence

| Area                               | Status                | Evidence                                                                                    |
| ---------------------------------- | --------------------- | ------------------------------------------------------------------------------------------- |
| Serialized check and reserve       | `VERIFIED_LOCAL`      | Unit/architecture tests exercise projected exposure, rollback, and single-owner transitions |
| Versioned limits                   | `VERIFIED_LOCAL`      | NEW → ADDED → STAGED → COMMITTED workflow and partial/lost update tests                     |
| Snapshot and journal-tail recovery | `VERIFIED_LOCAL`      | Replay uses the live handler in `PtrArchitectureTest`                                       |
| Lease/failover behavior            | `VERIFIED_LOCAL`      | Local lease-loss and standby-takeover tests/demo                                            |
| Live service and metrics surface   | `VERIFIED_LOCAL`      | Spring Boot demo and health endpoint exercised locally                                      |
| Replicated event bus/journal/lease | `DESIGNED_ONLY`       | In-process substitutes define interfaces but do not provide distributed guarantees          |
| Multi-node HA and DR               | `EXTERNAL_DEPENDENCY` | Requires representative cluster, failure domains, and durable infrastructure                |

## Production Gaps

| Workstream             | Production target                                                        | Required proof                                                                 |
| ---------------------- | ------------------------------------------------------------------------ | ------------------------------------------------------------------------------ |
| State ownership        | Strongly consistent partition ownership and fencing                      | Split-brain/partition tests showing stale owners cannot approve orders         |
| Durability             | Replicated ordered journal plus offset-bound snapshots                   | Crash at every acknowledgement boundary with zero unexplained reservations     |
| Performance            | Stated throughput and p99/p999 latency with allocation limits            | Load and soak tests on production-like hardware, including GC and burst tests  |
| Configuration          | Authenticated, approved, monotonic distribution with rollback            | Partial rollout, stale node, duplicate event, rollback, and audit tests        |
| Market/position inputs | Authoritative streams with freshness and gap policy                      | Feed delay/loss and reconciliation exercises                                   |
| Security               | Service identity, maker-checker, scoped authorization, secret management | Threat model, penetration review, access recertification, tamper-evident audit |
| Operations             | SLO dashboards, alerts, capacity alarms, runbooks, safe deployment       | On-call game days and rollback/failover drills                                 |
| DR/compliance          | Regional recovery and regulatory retention                               | Measured RPO/RTO exercise and retention/legal sign-off                         |

## Keep Outside the 60-Minute Answer

Do not implement or explain every infrastructure product, deployment manifest, schema, or dashboard during the interview. State the substitute and production mapping, then deep-dive only when asked. The interview must spend its time on atomic check/reserve, no database on the hot path, versioned limits, recovery, and fencing.

## Promotion Path

1. P0: agree SLOs, acknowledgement boundary, ownership model, threat model, and failure matrix.
2. P1: replace the in-process journal, lease, configuration bus, and market/position inputs with real adapters.
3. P2: run load/soak, crash consistency, split-brain, replay, and regional-recovery exercises.
4. P3: complete security, compliance, operational readiness, and controlled deployment review.
