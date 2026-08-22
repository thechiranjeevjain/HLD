# Architecture

## High-Level Design

```mermaid
flowchart LR
    C[Client] --> LB[Load Balancer]
    LB --> A[Stateless Aggregator]
    A --> I[(Idempotency Store)]
    A --> F[Concurrent Fan-Out]
    F --> S1[Service A]
    F --> S2[Service B]
    F --> S3[Service C]
    S1 & S2 & S3 --> M[Typed Merge]
    M --> I
    I --> C
```

Aggregator instances are stateless. A distributed unique request ID coordinates retries and races; each downstream has an independent connection pool, bulkhead, deadline, and breaker.

## Low-Level Design

```mermaid
sequenceDiagram
    participant Client
    participant Aggregator
    participant A
    participant B
    participant C
    participant Store
    Client->>Aggregator: aggregate(requestId)
    par concurrent calls
        Aggregator->>A: fetch(deadline)
        Aggregator->>B: fetch(deadline)
        Aggregator->>C: fetch(deadline)
    end
    A-->>Aggregator: OK
    B-->>Aggregator: ERROR
    C-->>Aggregator: TIMEOUT
    Aggregator->>Store: saveIfAbsent(partial)
    Store-->>Client: stored winner
```

`CompletableFuture` supplies fan-out concurrency, every call returns a typed `CallOutcome`, and `AggregateRepository.saveIfAbsent` is the seam for a database unique constraint.
