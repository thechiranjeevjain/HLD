# Production Readiness — Multi-Service Aggregator

Current target: **I1 — interview-ready**. The dependency-free service proves concurrent fan-out/fan-in and typed partial results. Read the shared [readiness definitions](../docs/READINESS_LEVELS.md).

## Current Evidence

| Area                             | Status                | Evidence                                                               |
| -------------------------------- | --------------------- | ---------------------------------------------------------------------- |
| Three-way concurrent calls       | `VERIFIED_LOCAL`      | Latency test proves max-of-three rather than sum-of-three behavior     |
| Retry and partial results        | `VERIFIED_LOCAL`      | Typed success/error/timeout tests                                      |
| Request idempotency              | `VERIFIED_LOCAL`      | Repeated ID returns the saved in-process winner                        |
| Local persistence                | `VERIFIED_LOCAL`      | CLI/HTTP demo appends aggregate JSON Lines                             |
| HTTP endpoint                    | `VERIFIED_LOCAL`      | Live request returned HTTP 200                                         |
| Distributed idempotency/database | `DESIGNED_ONLY`       | Repository seam exists; JSON Lines is not a transactional shared store |
| Real downstream services         | `EXTERNAL_DEPENDENCY` | Demo clients simulate latency and values                               |

## Production Gaps

| Workstream    | Production target                                                                      | Required proof                                                                 |
| ------------- | -------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------ |
| HTTP clients  | Nonblocking clients with connection pools, deadline propagation, and real cancellation | Timeout, half-open, DNS, connection-reset, and pool-saturation tests           |
| Resilience    | Per-downstream retry budget, jitter, circuit breaker, bulkhead, overload shedding      | Failure amplification, breaker recovery, queue saturation, and chaos tests     |
| Persistence   | Transactional store with unique request ID/hash and retention policy                   | Concurrent-instance race, restart, conflict, failure-before/after-write tests  |
| Semantics     | Versioned response schema and caller-specific completeness policy                      | Contract tests for complete, partial, stale fallback, and all-or-nothing modes |
| Scaling       | Stateless instances with bounded concurrency and autoscaling signals                   | Load/soak tests using downstream latency/error distributions                   |
| Security      | Authentication, subject authorization, service identity, PII controls                  | Threat model, penetration review, secret rotation, and log-redaction tests     |
| Observability | Traces and per-downstream latency/error/saturation metrics                             | Alert tuning, trace correlation, and incident game days                        |
| Deployment    | Safe migrations, canary/rollback, capacity and on-call ownership                       | Release rehearsal and production-readiness review                              |

## Keep Outside the 60-Minute Answer

Do not implement a complete service mesh, Kubernetes platform, or every resilience library during the interview. Focus on latency budgeting, concurrent calls, per-downstream isolation, retry safety, partial-result contract, and race-free idempotent persistence.

## Promotion Path

1. P0: agree API semantics, end-to-end deadline, downstream retryability, storage contract, SLOs, and threat/failure models.
2. P1: replace demo clients and JSON Lines with real async HTTP and transactional persistence.
3. P2: run load/soak, downstream chaos, connection saturation, race, restart, and data-recovery tests.
4. P3: complete security, observability, deployment safety, capacity sign-off, and controlled rollout.
