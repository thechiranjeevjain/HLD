# HLD 26–30: Runnable 40–60 Minute Interview Pack

Each topic is a separate runnable project or standalone module. The implementations are deliberately compact vertical slices: they execute the hardest correctness paths while the interview guides expand them into production-scale designs.

For learning order, interview ROI, readiness gates, and time-boxed preparation paths, use the canonical [owner learning roadmap](HLD-26-30-LEARNING-ROADMAP.md). This file remains the runnable project index and does not duplicate those rankings.

<!-- project-catalog:hld-index:start -->

| #   | Topic                                  | Canonical location                           | Runnable proof                                                                |
| --- | -------------------------------------- | -------------------------------------------- | ----------------------------------------------------------------------------- |
| 26  | Pre-Trade Risk Platform                | `trading-risk-platform/pretrade-risk-engine` | Hot-path check/reserve, dynamic config, journal/snapshot recovery, HA fencing |
| 27  | Exchange Connectivity Platform         | `exchange-connectivity-platform`             | Sessions, sequences, resend, throttle, failover, dedupe, uncertain outcome    |
| 28  | Market Data Platform                   | `market-data-platform`                       | Gap repair, normalization, book reconstruction, fan-out, slow consumers       |
| 29  | End-to-End Electronic Trading Platform | `electronic-trading-platform`                | Gateway → risk → OMS → connectivity → execution → positions → replay          |
| 30  | Multi-Service Aggregator               | `multi-service-aggregator`                   | Parallel calls, timeout/retry, partial results, idempotent persistence, HTTP  |

<!-- project-catalog:hld-index:end -->

## How Scope Is Separated

Use one canonical codebase per topic with two documentation tracks:

| Track      | Read when                                             | Stop condition                                                                                                |
| ---------- | ----------------------------------------------------- | ------------------------------------------------------------------------------------------------------------- |
| Interview  | Preparing or answering a 40–60 minute design question | Requirements, estimates, diagram, core flows, one or two deep dives, failures, trade-offs, runnable proof     |
| Production | Evaluating a real deployment                          | Real adapters, SLO evidence, load/soak, HA/recovery, security, operations, DR, compliance, controlled rollout |

The shared [readiness levels](docs/READINESS_LEVELS.md) define evidence labels and the I1–P4 ladder. Each project’s `PRODUCTION_READINESS.md` records its current proof and gaps. Production documents are roadmaps, not claims that the local simulator already provides those guarantees.

## Canonical Ownership and Redundancy

| Capability                                                               | Deep owner | What HLD 29 contains                                          |
| ------------------------------------------------------------------------ | ---------- | ------------------------------------------------------------- |
| Synchronous risk, limits, reservations, risk recovery                    | HLD 26     | A thin risk contract and end-to-end reaction to accept/reject |
| FIX/OUCH sessions, sequences, throttle, failover, unknown orders         | HLD 27     | A thin connectivity contract and normalized venue outcomes    |
| Feed recovery, normalization, books, fan-out                             | HLD 28     | A thin fresh-price/book contract and stale-data behavior      |
| OMS lifecycle, cross-service flow, executions, positions, reconciliation | HLD 29     | The full integration story                                    |
| Generic concurrent downstream aggregation                                | HLD 30     | Independent; do not force it into the trading platform        |

If an HLD 29 discussion reaches detailed risk, connectivity, or feed internals, link to the owning project instead of adding a second implementation. This keeps the integrated demo understandable and the subsystem projects deep.

## Run Everything

```powershell
cd G:\TechStudyNotes\SystemDesignProjects\trading-risk-platform
mvn -pl pretrade-risk-engine -am test

cd G:\TechStudyNotes\SystemDesignProjects\exchange-connectivity-platform
mvn test
mvn exec:java

cd G:\TechStudyNotes\SystemDesignProjects\market-data-platform
mvn test
mvn exec:java

cd G:\TechStudyNotes\SystemDesignProjects\electronic-trading-platform
mvn test
mvn exec:java

cd G:\TechStudyNotes\SystemDesignProjects\multi-service-aggregator
mvn test
mvn exec:java
```

## How to Use This Pack

1. Practice the timed guide without code first: clarify, estimate, draw, deep dive, failover, trade-offs.
2. Run the demo and connect each printed state transition to one box or arrow in the design.
3. Run the failure tests and explain the invariant each protects.
4. Clearly separate what the local simulator proves from distributed production guarantees.

Do not merge these into one giant project, and do not fork separate “interview” and “production” codebases. Promote the same boundaries with real adapters and stronger evidence only when a production objective exists.
