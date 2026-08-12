# Architecture

## High level

```mermaid
flowchart LR
 U["React browser"] -->|REST and STOMP| B["Spring Boot modular monolith"]
 B --> P[(PostgreSQL)]
 B -. coordination seam .-> R[(Redis)]
 B --> L["Fake or OpenAI LLM"]
 B --> W["Docker sandbox manager"]
 W --> S["Non-root sandbox"]
 S --> G["Git, file, build, test tools"]
```

The backend owns orchestration, authorization, budgets, checkpoints, and credentials. Untrusted repository data stays data; it cannot redefine system policy. The sandbox owns all agent-generated command execution.

## Execution and sandbox lifecycle

```mermaid
sequenceDiagram
 Browser->>Backend: POST session with idempotent intent
 Backend->>PostgreSQL: persist CREATED
 Backend->>Sandbox: allocate constrained container
 Sandbox->>Sandbox: clone repository
 Backend->>LLM: task plus bounded context
 LLM-->>Backend: typed plan
 loop maximum 20 steps
  Backend->>Sandbox: one validated tool call
  Backend->>PostgreSQL: durable audit result
  Backend-->>Browser: best-effort WebSocket event
 end
 Backend->>Sandbox: build, test, git diff
 Backend->>PostgreSQL: terminal state and summary
 Backend->>Sandbox: forced cleanup
```

```mermaid
flowchart TD
 A["Create workspace"] --> B["Start non-root, limited container"] --> C["Clone"] --> D["Execute validated calls"] --> E["Capture diff"] --> F["docker rm -f"] --> G["Persist cleanup result"]
```

## State and recovery

```mermaid
stateDiagram-v2
 [*] --> CREATED
 CREATED --> ALLOCATING
 ALLOCATING --> PLANNING
 PLANNING --> EXECUTING
 EXECUTING --> VALIDATING
 VALIDATING --> COMPLETED
 CREATED --> CANCELLED
 EXECUTING --> CANCELLED
 ALLOCATING --> FAILED
 PLANNING --> FAILED
 EXECUTING --> FAILED
 VALIDATING --> FAILED
```

PostgreSQL is the truth. Optimistic locking rejects concurrent stale updates; `(session_id, sequence_no)` deduplicates step writes. A production recovery scanner would lease non-terminal checkpoints through Redis and resume only idempotent steps. External effects use idempotency keys; execution is at-least-once, never assumed exactly-once.

```mermaid
flowchart LR
 F["Worker failure"] --> DB["Read durable checkpoint"] --> C{"Effect recorded?"}
 C -->|yes| N["Advance without repeating"]
 C -->|no, retryable| R["Exponential backoff plus jitter"]
 C -->|no, permanent| X["FAILED with reason"]
```

## WebSocket flow

```mermaid
sequenceDiagram
 Backend->>PostgreSQL: commit state or step
 Backend-->>Browser: publish live event
 Browser--xBackend: disconnect
 Browser->>Backend: GET session and steps
 Backend-->>Browser: authoritative reconstruction
```

Kafka is unnecessary while one database transaction and a bounded worker pool are enough. Add an outbox plus Kafka when multiple independent consumers need replayable high-volume events, cross-region buffering, or long-running workflow fan-out.
