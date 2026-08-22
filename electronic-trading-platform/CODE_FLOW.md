# End-to-End Electronic Trading Platform Code Flow

This is the single code-flow guide for the project. It connects the business request to concrete source files, explains both architectural levels, and shows where validation, state changes, asynchronous work, and responses occur.

## Scope and outcome

A runnable Java 17 vertical slice from authenticated client order through market data, synchronous risk, OMS idempotency, venue connectivity, execution handling, positions, recovery, and metrics.

The tracked production-code inventory used by this guide contains **1 source units** and **0 annotation-discovered HTTP operations**. Generated/build output and test sources are intentionally excluded from the runtime path.

## High-Level Design

At the system level, callers enter through an inbound adapter. The adapter owns transport concerns; application/domain code owns use-case decisions; repositories and gateways isolate state and external systems. Asynchronous adapters extend the flow without moving business invariants into controllers or consumers.

```mermaid
flowchart LR
    Caller["Client / operator / upstream system"] --> Inbound["ElectronicTradingPlatform"]
    Inbound --> Domain["ElectronicTradingPlatform"]
    Domain --> Store["State"]
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
    participant Inbound as ElectronicTradingPlatform
    participant Domain as ElectronicTradingPlatform
    participant Store as State
    Caller->>Inbound: submit input
    Inbound->>Inbound: parse and boundary-validate
    Inbound->>Domain: invoke use case
    Domain->>Domain: enforce invariants and make decision
    Domain-->>Inbound: domain result or typed failure
    Inbound-->>Caller: mapped response / output
```

### Component map

| Responsibility   | Concrete code               |
| ---------------- | --------------------------- |
| Supporting logic | `ElectronicTradingPlatform` |

### Inbound operations

| Verb/trigger | Path or input                                                                                                        | Owning code             |
| ------------ | -------------------------------------------------------------------------------------------------------------------- | ----------------------- |
| N/A          | No annotation-based HTTP endpoint; execution starts through the process API, CLI, test harness, or protocol adapter. | See entry points below. |

## Detailed source walkthrough

Read this table top-down by category, then follow the linked files. “Key methods” are discovered from the checked-in implementation and identify useful debugging/interview entry points.

| Source file                                                                                              | Role             | Responsibility and important methods                                                                                                                                                       |
| -------------------------------------------------------------------------------------------------------- | ---------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| [`ElectronicTradingPlatform.java`](./src/main/java/com/techstudy/trading/ElectronicTradingPlatform.java) | Supporting logic | ElectronicTradingPlatform provides a focused algorithm or shared implementation detail. Key methods: `OrderRequest()`, `OrderView()`, `Quote()`, `TradingEvent()`, `onQuote()`, `quote()`. |

## End-to-end code-flow narrative

1. Start at `ElectronicTradingPlatform`. It receives the external input and should perform only boundary parsing, authentication/authorization handoff, and request validation.
2. Follow the call into `ElectronicTradingPlatform`. This is the principal place to explain the use case, invariant checks, deduplication/concurrency decision, and success/failure result.
3. Continue into `State` for the durable or in-memory state transition. Transaction and consistency guarantees belong at this boundary, not in response mapping.
4. This flow completes synchronously; background work is not part of the primary checked-in path.
5. External-system behavior is either absent or represented behind another listed adapter.
6. Return to `ElectronicTradingPlatform`, where typed outcomes become the public response or protocol result. Logging and metrics should preserve correlation identifiers without leaking secrets.

## Failure and correctness checkpoints

- **Invalid input:** rejected at the inbound contract before state mutation.
- **Domain conflict:** returned as a typed result; do not hide it as an infrastructure exception.
- **Duplicate or concurrent work:** handled where the project’s idempotency, locking, ordering, or atomic data structure is implemented.
- **Storage/integration failure:** transaction is rolled back or the operation remains safely retryable.
- **Async failure:** consumer retries must preserve idempotency; poison work must become observable rather than loop silently.
- **Observability:** trace the same request/correlation identity through inbound, domain, persistence, and async logs.

## How to trace a scenario in the debugger

1. Choose one operation from the inbound-operations table or the project’s demo script.
2. Break at `ElectronicTradingPlatform`, then step into `ElectronicTradingPlatform` rather than framework internals.
3. Inspect the command/request object immediately after validation.
4. Stop before and after the call to `State` to compare intended and durable state.
5. If messaging exists, capture the emitted identifier and continue from `the consumer/worker`.
6. Confirm the final public result and the corresponding logs/metrics.

## Related project documentation

- [Project README](./README.md)
- [Architecture details](./docs/ARCHITECTURE.md)
- [Interview guide](./INTERVIEW_GUIDE.md)
