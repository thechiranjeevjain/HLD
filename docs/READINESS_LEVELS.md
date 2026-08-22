# Interview Scope and Production Readiness

“Interview-ready” and “production-ready” answer different questions. A project can be excellent for a 60-minute interview and still be far from safe deployment.

## Two Separate Tracks

| Track            | Purpose                                                                                       | Required evidence                                                                                                            |
| ---------------- | --------------------------------------------------------------------------------------------- | ---------------------------------------------------------------------------------------------------------------------------- |
| Interview track  | Explain the design, defend trade-offs, and demonstrate its hardest invariant in 40–60 minutes | Timed guide, architecture diagrams, runnable vertical slice, focused tests, failure discussion                               |
| Production track | Establish that the system can safely carry real traffic under expected load and failures      | Real infrastructure, protocol conformance, durability, HA tests, load/soak tests, security review, operability, DR exercises |

The tracks share domain code and architecture decisions. They do not share the same completion claim.

## Evidence Labels

Use these labels in documentation and reviews:

| Label                    | Meaning                                                                                          |
| ------------------------ | ------------------------------------------------------------------------------------------------ |
| `VERIFIED_LOCAL`         | Executed in the current workspace with recorded tests or smoke evidence                          |
| `IMPLEMENTED_UNVERIFIED` | Code/config exists, but the relevant runtime or environment was not exercised                    |
| `DESIGNED_ONLY`          | Architecture or interface is documented; no implementation claim                                 |
| `EXTERNAL_DEPENDENCY`    | Requires a venue, feed, cluster, security system, cloud account, or other unavailable dependency |

## Readiness Ladder

| Level                            | Exit criteria                                                                                                                |
| -------------------------------- | ---------------------------------------------------------------------------------------------------------------------------- |
| I1 — Interview-ready             | Coherent 60-minute answer, runnable key invariant, focused tests, explicit trade-offs and boundaries                         |
| P0 — Production design candidate | SLOs, capacity model, data ownership, threat model, failure matrix, rollout/rollback, and operational review exist           |
| P1 — Integrated candidate        | Real persistence/protocol/infrastructure adapters run in a representative environment                                        |
| P2 — Resilience candidate        | Load, soak, fault injection, failover, recovery, and DR evidence meet stated targets                                         |
| P3 — Release candidate           | Security/compliance review, dashboards/alerts/runbooks, deployment safety, on-call ownership, and capacity sign-off complete |
| P4 — Production-proven           | Controlled rollout and real-traffic evidence meet SLOs over an agreed observation period                                     |

Passing I1 is not partial failure. It is the intended finish line for this interview portfolio. Production work starts from P0 only when a real deployment objective exists.

## Anti-Redundancy Rule

One project owns each deep subsystem. An integrating project uses a small boundary implementation and links to the owner:

- HLD 26 owns synchronous risk state, reservations, limits, and risk recovery.
- HLD 27 owns venue sessions, protocol sequencing, throttling, failover, and uncertain outcomes.
- HLD 28 owns feed sequencing, recovery, normalization, books, and fan-out.
- HLD 29 owns the end-to-end lifecycle and integration contracts. It must not grow second full implementations of HLD 26–28.
- HLD 30 owns general concurrent aggregation and downstream resilience; it is independent of the trading stack.

This prevents one oversized project while preserving an end-to-end story.
