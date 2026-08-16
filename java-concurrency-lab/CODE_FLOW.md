# Java Concurrency Lab Code Flow

This is the single code-flow guide for the project. It connects the business request to concrete source files, explains both architectural levels, and shows where validation, state changes, asynchronous work, and responses occur.

## Scope and outcome

A Java 21 interview lab that answers one practical question: **how does a backend process 100,000 concurrent stock-order requests without corrupting state or losing latency control?** It first makes the failures visible, then runs the safe design.

The tracked production-code inventory used by this guide contains **11 source units** and **0 annotation-discovered HTTP operations**. Generated/build output and test sources are intentionally excluded from the runtime path.

## High-Level Design

At the system level, callers enter through an inbound adapter. The adapter owns transport concerns; application/domain code owns use-case decisions; repositories and gateways isolate state and external systems. Asynchronous adapters extend the flow without moving business invariants into controllers or consumers.

```mermaid
flowchart LR
    Caller["Client / operator / upstream system"] --> Inbound["AsyncRiskChecks"]
    Inbound --> Domain["ConcurrentOrderProcessor"]
    Domain --> Store["Order"]
    Domain --> Result["Response / observable result"]
```

### Runtime stages

1. **Enter:** a request, command, scheduled trigger, protocol message, or UI action reaches the inbound boundary.
2. **Validate:** transport shape and required fields are rejected before domain mutation.
3. **Decide:** application/domain logic loads required state and applies invariants, idempotency, authorization, limits, or algorithms.
4. **Commit:** durable state changes pass through a repository/store; external calls pass through gateways; asynchronous work passes through message boundaries.
5. **Return and observe:** the adapter maps the result to an HTTP response, protocol response, CLI output, event, or metric.

## Low-Level Design

The low-level path keeps orchestration directional: inbound adapter → application/domain unit → persistence/outbound adapter. Contracts carry data between layers; configuration and security apply cross-cutting policy without becoming business logic.

```mermaid
sequenceDiagram
    autonumber
    actor Caller
    participant Inbound as AsyncRiskChecks
    participant Domain as ConcurrentOrderProcessor
    participant Store as Order
    Caller->>Inbound: submit input
    Inbound->>Inbound: parse and boundary-validate
    Inbound->>Domain: invoke use case
    Domain->>Domain: enforce invariants and make decision
    Domain->>Store: read or persist state
    Store-->>Domain: current durable result
    Domain-->>Inbound: domain result or typed failure
    Inbound-->>Caller: mapped response / output
```

### Component map

| Responsibility           | Concrete code                                                                                                                           |
| ------------------------ | --------------------------------------------------------------------------------------------------------------------------------------- |
| Supporting logic         | `AsyncRiskChecks`, `ExecutorMonitor`, `FailureDemos`, `LoadGenerator`, `RiskState`, `CounterBenchmark`, `LockBenchmark`, `MapBenchmark` |
| Application/domain logic | `ConcurrentOrderProcessor`, `UnsafeOrderProcessor`                                                                                      |
| Domain/data model        | `Order`                                                                                                                                 |

### Inbound operations

| Verb/trigger | Path or input                                                                                                        | Owning code             |
| ------------ | -------------------------------------------------------------------------------------------------------------------- | ----------------------- |
| N/A          | No annotation-based HTTP endpoint; execution starts through the process API, CLI, test harness, or protocol adapter. | See entry points below. |

## Detailed source walkthrough

Read this table top-down by category, then follow the linked files. “Key methods” are discovered from the checked-in implementation and identify useful debugging/interview entry points.

| Source file                                                                                                | Role                     | Responsibility and important methods                                                                                                                                                                                                                    |
| ---------------------------------------------------------------------------------------------------------- | ------------------------ | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| [`AsyncRiskChecks.java`](./src/main/java/dev/interview/concurrency/AsyncRiskChecks.java)                   | Supporting logic         | AsyncRiskChecks provides a focused algorithm or shared implementation detail. Key methods: `validate()`.                                                                                                                                                |
| [`ConcurrentOrderProcessor.java`](./src/main/java/dev/interview/concurrency/ConcurrentOrderProcessor.java) | Application/domain logic | ConcurrentOrderProcessor coordinates the use case and enforces domain decisions. Key methods: `Result()`, `submit()`, `state()`, `exposureSnapshot()`, `submitted()`, `accepted()`.                                                                     |
| [`ExecutorMonitor.java`](./src/main/java/dev/interview/concurrency/ExecutorMonitor.java)                   | Supporting logic         | ExecutorMonitor provides a focused algorithm or shared implementation detail. Key methods: `Snapshot()`, `snapshot()`, `report()`.                                                                                                                      |
| [`FailureDemos.java`](./src/main/java/dev/interview/concurrency/FailureDemos.java)                         | Supporting logic         | FailureDemos provides a focused algorithm or shared implementation detail. Key methods: `lostUpdates()`, `hashMapCorruption()`, `deadlockDetected()`, `starvationDetected()`, `unboundedOverloadQueueSize()`, `boundedCallerRunsAppliesBackpressure()`. |
| [`LoadGenerator.java`](./src/main/java/dev/interview/concurrency/LoadGenerator.java)                       | Supporting logic         | LoadGenerator provides a focused algorithm or shared implementation detail. Key methods: `main()`.                                                                                                                                                      |
| [`Order.java`](./src/main/java/dev/interview/concurrency/Order.java)                                       | Domain/data model        | Order represents domain state, identity, or an invariant-bearing value. Key methods: `signedNotionalCents()`.                                                                                                                                           |
| [`RiskState.java`](./src/main/java/dev/interview/concurrency/RiskState.java)                               | Supporting logic         | RiskState provides a focused algorithm or shared implementation detail. Key methods: `empty()`, `accepted()`, `rejected()`.                                                                                                                             |
| [`UnsafeOrderProcessor.java`](./src/main/java/dev/interview/concurrency/UnsafeOrderProcessor.java)         | Application/domain logic | UnsafeOrderProcessor coordinates the use case and enforces domain decisions. Key methods: `process()`, `processWithReadBarrier()`, `exposure()`, `processed()`, `clientCount()`.                                                                        |
| [`CounterBenchmark.java`](./src/main/java/dev/interview/concurrency/benchmarks/CounterBenchmark.java)      | Supporting logic         | CounterBenchmark provides a focused algorithm or shared implementation detail. Key methods: `atomicLong()`, `longAdder()`.                                                                                                                              |
| [`LockBenchmark.java`](./src/main/java/dev/interview/concurrency/benchmarks/LockBenchmark.java)            | Supporting logic         | LockBenchmark provides a focused algorithm or shared implementation detail. Key methods: `synchronizedLock()`, `reentrantLock()`.                                                                                                                       |
| [`MapBenchmark.java`](./src/main/java/dev/interview/concurrency/benchmarks/MapBenchmark.java)              | Supporting logic         | MapBenchmark provides a focused algorithm or shared implementation detail. Key methods: `hashMapSingleThread()`, `concurrentHashMap()`.                                                                                                                 |

## End-to-end code-flow narrative

1. Start at `AsyncRiskChecks`. It receives the external input and should perform only boundary parsing, authentication/authorization handoff, and request validation.
2. Follow the call into `ConcurrentOrderProcessor`. This is the principal place to explain the use case, invariant checks, deduplication/concurrency decision, and success/failure result.
3. Continue into `Order` for the durable or in-memory state transition. Transaction and consistency guarantees belong at this boundary, not in response mapping.
4. This flow completes synchronously; background work is not part of the primary checked-in path.
5. External-system behavior is either absent or represented behind another listed adapter.
6. Return to `AsyncRiskChecks`, where typed outcomes become the public response or protocol result. Logging and metrics should preserve correlation identifiers without leaking secrets.

## Failure and correctness checkpoints

- **Invalid input:** rejected at the inbound contract before state mutation.
- **Domain conflict:** returned as a typed result; do not hide it as an infrastructure exception.
- **Duplicate or concurrent work:** handled where the project’s idempotency, locking, ordering, or atomic data structure is implemented.
- **Storage/integration failure:** transaction is rolled back or the operation remains safely retryable.
- **Async failure:** consumer retries must preserve idempotency; poison work must become observable rather than loop silently.
- **Observability:** trace the same request/correlation identity through inbound, domain, persistence, and async logs.

## How to trace a scenario in the debugger

1. Choose one operation from the inbound-operations table or the project’s demo script.
2. Break at `AsyncRiskChecks`, then step into `ConcurrentOrderProcessor` rather than framework internals.
3. Inspect the command/request object immediately after validation.
4. Stop before and after the call to `Order` to compare intended and durable state.
5. If messaging exists, capture the emitted identifier and continue from `the consumer/worker`.
6. Confirm the final public result and the corresponding logs/metrics.

## Related project documentation

- [Project README](./README.md)
- [Specialized diagrams](./docs/DIAGRAMS.md)
- [Interview guide](./docs/INTERVIEW_GUIDE.md)
