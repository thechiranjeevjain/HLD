# Java Concurrency Lab Interview Guide

## Two-Minute Pitch

This lab teaches backend concurrency through a stock-order processor. It first makes common failures visible, then runs a safe design that processes 100,000 orders with bounded executor capacity, asynchronous validation, per-client locking, atomic counters, concurrent maps, and immutable state snapshots.

## What To Emphasize

- The lab is about concurrency mechanics, not building another risk product.
- Broken demos show lost updates, unsafe `HashMap` mutation, deadlock, starvation, overload, and lock contention.
- `ThreadPoolExecutor` makes capacity explicit through pool size and queue limits.
- `CallerRunsPolicy` applies backpressure instead of hiding overload in an unbounded queue.
- `CompletableFuture` composes independent checks without nested worker blocking.
- A short per-client `ReentrantLock` protects the business invariant.
- `AtomicReference<RiskState>` safely publishes complete immutable snapshots.

## Safe Request Flow

1. `LoadGenerator` submits seeded orders from many clients.
2. `ConcurrentOrderProcessor` admits work to a bounded executor.
3. `AsyncRiskChecks` runs independent validation stages.
4. The processor acquires the client lock for the check-then-update exposure invariant.
5. Accepted orders update sequence, exposure, metrics, and the published snapshot.
6. `ExecutorMonitor` exposes active, queued, completed, and submitted work.

## Tradeoffs

| Decision | Benefit | Cost |
| --- | --- | --- |
| Per-client lock | Serializes only the affected business invariant | Hot clients can still contend |
| Bounded executor | Predictable capacity and failure mode | Callers feel backpressure under load |
| Caller-runs rejection | Slows producers naturally | Request thread may spend time doing work |
| Immutable `RiskState` | Readers never observe partial updates | More object allocation |
| `LongAdder` metrics | High throughput counters | `sum()` is observational, not transactional |
| Failure-first demos | Strong interview teaching value | Some demos intentionally print warnings |

## FAQ

Q: Why not just use `ConcurrentHashMap` for everything?
A: It protects map internals and per-key operations. It does not automatically make a multi-step business invariant atomic.

Q: Why does the failure demo print Maven lingering-thread warnings?
A: The deadlock and starvation demonstrations intentionally create stuck work so the failure is visible. The command still exits successfully.

Q: Why Java 21?
A: It is current enough for backend interviews and keeps the lab aligned with modern Maven/JDK workflows. This machine validated it using a newer JDK that can compile release 21 code.

Q: What would you add next?
A: virtual-thread comparison, structured concurrency examples, lock fairness measurements, JFR recordings checked into docs, and a small HTTP wrapper for live dashboards.
