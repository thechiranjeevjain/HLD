# 40–60 Minute Interview Guide — Multi-Service Aggregator

## What a Strong Final Answer Contains

The board should show one API calling three independent services concurrently, an end-to-end deadline split into per-call budgets, retries only where safe, partial-result semantics, idempotent final persistence, bulkheads/circuit breakers, horizontal scaling, and observability.

## Timed Plan

| Time      | Output                                                                           |
| --------- | -------------------------------------------------------------------------------- |
| 0–5 min   | Clarify response contract, latency SLO, persistence, and downstream idempotency  |
| 5–10 min  | Estimate QPS, concurrency, connection pools, payload/storage, and timeout budget |
| 10–20 min | Draw API → idempotency → concurrent fan-out → merge → store                      |
| 20–30 min | Walk success and define API/data model                                           |
| 30–40 min | Timeouts, retries, partial failure, circuit breakers, and bulkheads              |
| 40–50 min | Race-free persistence, scaling, backpressure, and caching                        |
| 50–60 min | Observability, security, trade-offs, bottlenecks, recap                          |

## 1. Clarify Scope

Ask:

- Must all three answers be present, or are partial results valid?
- Are downstream calls reads and safe to retry?
- Is persistence required before replying? Can the same request ID be retried?
- Overall latency SLO and individual downstream p95/p99?
- May stale cached data substitute for an outage?
- Does the caller need synchronous response, job polling, or both?

Assume 10k requests/s peak, 300 ms end-to-end p99, three read-only downstream calls, partial results allowed, final result persisted before response, and a 24-hour idempotency window.

## 2. Requirements

Functional: validate request, deduplicate request ID, call A/B/C concurrently, apply deadline/retry policy, aggregate typed results, persist once, return complete or partial response, and allow later lookup.

Non-functional: bounded latency, no thread/connection exhaustion, horizontally scalable stateless API, durable/idempotent writes, isolated failures, traceability, and explicit overload behavior.

Define HTTP semantics: 200 for complete, 206 (or a domain-specific 200 envelope) for an accepted partial result, 4xx for bad request/auth, 503 for overload, and 5xx when required persistence fails.

## 3. Capacity Sketch

- 10k inbound RPS creates up to 30k downstream RPS before retries.
- At 200 ms average, Little’s Law gives about 6,000 concurrent downstream requests; size async clients, sockets, and bulkheads accordingly.
- A single retry can double load during an incident, so use retry budgets and jitter.
- 10k results/s × 5 KB ≈ 50 MB/s raw writes before indexes/replicas; choose TTL/archive policy.
- Scale aggregator instances behind a load balancer; request ID, not instance affinity, provides dedupe.

## 4. API and Data Model

```http
POST /v1/aggregates
Idempotency-Key: request-123

{ "subjectId": "customer-7" }
```

```json
{
  "requestId": "request-123",
  "complete": false,
  "results": {
    "profile": { "status": "OK", "value": {} },
    "pricing": { "status": "TIMEOUT" },
    "inventory": { "status": "OK", "value": {} }
  }
}
```

Persist `request_id` (unique), request hash, status, per-service status/value/error, attempt count, timestamps, schema version, and expiry. Reject reuse of one key with a different request hash.

## 5. Architecture and Success Flow

1. Load balancer authenticates/rate-limits and sends to a stateless aggregator.
2. Service checks the idempotency store. Completed request returns immediately; in-progress request waits/polls according to contract.
3. Deadline context is created from the client/server budget.
4. Three nonblocking calls start concurrently, each in its own logical bulkhead/connection pool.
5. Results become typed envelopes; the merger never waits past the overall deadline.
6. Validate/transform the aggregate and insert it with a unique request ID.
7. Return the stored winner so concurrent identical requests see the same result.

Latency is approximately gateway + max(A, B, C) + merge + persistence, not A + B + C.

## 6. Resilience Deep Dive

### Timeouts and cancellation

Give every call a deadline no later than the remaining overall budget. Cancel or discard late work and use an HTTP client that actually aborts requests/connections safely. Leave time for merge and persistence. Propagate deadlines downstream.

### Retries

Retry only idempotent transient failures (connect reset, selected 5xx/429) within the remaining budget. Use exponential backoff with jitter, cap attempts, honor `Retry-After`, and enforce a global retry budget. Do not retry validation errors or deterministic not-found results.

### Circuit breaker and bulkhead

Maintain separate breaker state and concurrency limits per downstream. A slow inventory service must not consume every worker/socket needed by profile and pricing. Breakers fail fast and probe recovery; bounded queues return overload rather than exhaust memory.

### Partial results

Return a status per service and make completeness machine-readable. If business requires all-or-nothing, fail the aggregate but still record diagnostic outcomes. Optional stale-cache fallback must include age/source.

## 7. Persistence and Races

Use a database unique constraint on `request_id`. Options:

- Insert `IN_PROGRESS`, then update once after merge; competing callers observe/poll the same row.
- Compute first, then `INSERT ... ON CONFLICT` and return the winning row. This may duplicate downstream reads but not the stored outcome.
- For long jobs, write command to a durable queue and expose `202 + status URL`.

Do not use an in-memory lock for distributed idempotency. If final persistence is mandatory and fails, do not return success. Consider an outbox when another event must be published atomically with the stored result.

## 8. Scaling and Backpressure

Use async I/O or carefully bounded executors, tune connection pools per host, cap in-flight work, shed load before saturation, and autoscale on concurrency/latency rather than CPU alone. Cache immutable or slow-changing downstream data with TTL and request coalescing, but define staleness.

## 9. Observability and Security

Record overall and per-downstream latency, status/error class, attempts, timeout rate, breaker state, bulkhead/connection utilization, queue wait, complete/partial ratio, persistence latency/conflicts, cache age/hit rate, and request rate. Propagate trace context and tag spans by downstream—not high-cardinality user IDs.

Authenticate callers, authorize subject access, use mTLS/service identity, validate payloads, encrypt stored results, redact secrets/PII from logs, and rate-limit abusive keys.

## 10. Trade-Offs

- Synchronous aggregation is simple but pins the client to the slowest allowed dependency; use async jobs for long work.
- Partial results improve availability but push interpretation complexity to callers.
- Retries improve isolated transients but amplify broad outages.
- Compute-before-insert reduces row contention but can duplicate calls; reserve-first improves dedupe but needs stuck-job recovery.
- Caching cuts latency/load but introduces staleness and invalidation decisions.

## 11. Runnable Proof

Run `mvn test`, `mvn exec:java`, or the HTTP server command in the README. Tests prove max-not-sum concurrency, a successful retry, idempotent persistence, timeout, error, and partial output. State clearly that JSON Lines is the local persistence adapter; production uses a transactional store with a unique request ID.
