# Product Catalog API Code Flow

This is the single code-flow guide for the project. It connects the business request to concrete source files, explains both architectural levels, and shows where validation, state changes, asynchronous work, and responses occur.

## Scope and outcome

A Spring Boot product catalog API with PostgreSQL persistence, Flyway migrations, validation, centralized error responses, and Docker Compose for local runtime.

The tracked production-code inventory used by this guide contains **12 source units** and **5 annotation-discovered HTTP operations**. Generated/build output and test sources are intentionally excluded from the runtime path.

## High-Level Design

At the system level, callers enter through an inbound adapter. The adapter owns transport concerns; application/domain code owns use-case decisions; repositories and gateways isolate state and external systems. Asynchronous adapters extend the flow without moving business invariants into controllers or consumers.

```mermaid
flowchart LR
    Caller["Client / operator / upstream system"] --> Inbound["GlobalExceptionHandler"]
    Inbound --> Domain["ProductService"]
    Domain --> Store["ProductRepository"]
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
    participant Inbound as GlobalExceptionHandler
    participant Domain as ProductService
    participant Store as ProductRepository
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

| Responsibility           | Concrete code                                                     |
| ------------------------ | ----------------------------------------------------------------- |
| Entry point              | `ProductCrudApplication`                                          |
| Supporting logic         | `ApiError`, `DuplicateSkuException`, `NotFoundException`          |
| Inbound adapter          | `GlobalExceptionHandler`, `ProductController`                     |
| API/message contract     | `CreateProductRequest`, `ProductResponse`, `UpdateProductRequest` |
| Domain/data model        | `Product`                                                         |
| Persistence adapter      | `ProductRepository`                                               |
| Application/domain logic | `ProductService`                                                  |

### Inbound operations

| Verb/trigger | Path or input                | Owning code         |
| ------------ | ---------------------------- | ------------------- |
| `GET`        | `(class-level/default path)` | `ProductController` |
| `GET`        | `/{id}`                      | `ProductController` |
| `POST`       | `(class-level/default path)` | `ProductController` |
| `PUT`        | `/{id}`                      | `ProductController` |
| `DELETE`     | `/{id}`                      | `ProductController` |

## Detailed source walkthrough

Read this table top-down by category, then follow the linked files. “Key methods” are discovered from the checked-in implementation and identify useful debugging/interview entry points.

| Source file                                                                                                  | Role                     | Responsibility and important methods                                                                                                                                                                                                        |
| ------------------------------------------------------------------------------------------------------------ | ------------------------ | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| [`ProductCrudApplication.java`](./src/main/java/com/example/capstone/crud/ProductCrudApplication.java)       | Entry point              | ProductCrudApplication bootstraps the process and wires the runtime. Key methods: `main()`.                                                                                                                                                 |
| [`ApiError.java`](./src/main/java/com/example/capstone/crud/error/ApiError.java)                             | Supporting logic         | ApiError provides a focused algorithm or shared implementation detail. Key methods: `of()`, `withFields()`.                                                                                                                                 |
| [`DuplicateSkuException.java`](./src/main/java/com/example/capstone/crud/error/DuplicateSkuException.java)   | Supporting logic         | DuplicateSkuException provides a focused algorithm or shared implementation detail.                                                                                                                                                         |
| [`GlobalExceptionHandler.java`](./src/main/java/com/example/capstone/crud/error/GlobalExceptionHandler.java) | Inbound adapter          | GlobalExceptionHandler accepts an inbound call, validates its boundary contract, and delegates work. Key methods: `handleNotFound()`, `handleDuplicateSku()`, `handleValidation()`, `handleConstraintViolation()`, `handleDataIntegrity()`. |
| [`NotFoundException.java`](./src/main/java/com/example/capstone/crud/error/NotFoundException.java)           | Supporting logic         | NotFoundException provides a focused algorithm or shared implementation detail.                                                                                                                                                             |
| [`CreateProductRequest.java`](./src/main/java/com/example/capstone/crud/product/CreateProductRequest.java)   | API/message contract     | CreateProductRequest carries validated data across an API or messaging boundary.                                                                                                                                                            |
| [`Product.java`](./src/main/java/com/example/capstone/crud/product/Product.java)                             | Domain/data model        | Product represents domain state, identity, or an invariant-bearing value. Key methods: `replaceWith()`, `getId()`, `getSku()`, `getName()`, `getDescription()`, `getPrice()`.                                                               |
| [`ProductController.java`](./src/main/java/com/example/capstone/crud/product/ProductController.java)         | Inbound adapter          | ProductController accepts an inbound call, validates its boundary contract, and delegates work. Key methods: `list()`, `get()`, `create()`, `update()`, `delete()`.                                                                         |
| [`ProductRepository.java`](./src/main/java/com/example/capstone/crud/product/ProductRepository.java)         | Persistence adapter      | ProductRepository reads or writes durable state behind a storage boundary.                                                                                                                                                                  |
| [`ProductResponse.java`](./src/main/java/com/example/capstone/crud/product/ProductResponse.java)             | API/message contract     | ProductResponse carries validated data across an API or messaging boundary. Key methods: `from()`.                                                                                                                                          |
| [`ProductService.java`](./src/main/java/com/example/capstone/crud/product/ProductService.java)               | Application/domain logic | ProductService coordinates the use case and enforces domain decisions. Key methods: `list()`, `get()`, `create()`, `update()`, `delete()`.                                                                                                  |
| [`UpdateProductRequest.java`](./src/main/java/com/example/capstone/crud/product/UpdateProductRequest.java)   | API/message contract     | UpdateProductRequest carries validated data across an API or messaging boundary.                                                                                                                                                            |

## End-to-end code-flow narrative

1. Start at `GlobalExceptionHandler`. It receives the external input and should perform only boundary parsing, authentication/authorization handoff, and request validation.
2. Follow the call into `ProductService`. This is the principal place to explain the use case, invariant checks, deduplication/concurrency decision, and success/failure result.
3. Continue into `ProductRepository` for the durable or in-memory state transition. Transaction and consistency guarantees belong at this boundary, not in response mapping.
4. This flow completes synchronously; background work is not part of the primary checked-in path.
5. External-system behavior is either absent or represented behind another listed adapter.
6. Return to `GlobalExceptionHandler`, where typed outcomes become the public response or protocol result. Logging and metrics should preserve correlation identifiers without leaking secrets.

## Failure and correctness checkpoints

- **Invalid input:** rejected at the inbound contract before state mutation.
- **Domain conflict:** returned as a typed result; do not hide it as an infrastructure exception.
- **Duplicate or concurrent work:** handled where the project’s idempotency, locking, ordering, or atomic data structure is implemented.
- **Storage/integration failure:** transaction is rolled back or the operation remains safely retryable.
- **Async failure:** consumer retries must preserve idempotency; poison work must become observable rather than loop silently.
- **Observability:** trace the same request/correlation identity through inbound, domain, persistence, and async logs.

## How to trace a scenario in the debugger

1. Choose one operation from the inbound-operations table or the project’s demo script.
2. Break at `GlobalExceptionHandler`, then step into `ProductService` rather than framework internals.
3. Inspect the command/request object immediately after validation.
4. Stop before and after the call to `ProductRepository` to compare intended and durable state.
5. If messaging exists, capture the emitted identifier and continue from `the consumer/worker`.
6. Confirm the final public result and the corresponding logs/metrics.

## Related project documentation

- [Project README](./README.md)
- [Specialized diagrams](./docs/DIAGRAMS.md)
- [Interview guide](./docs/INTERVIEW_GUIDE.md)
