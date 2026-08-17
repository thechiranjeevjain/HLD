# Revision Sheet

**Architecture:** React → REST/STOMP → Spring Boot modular monolith → PostgreSQL; orchestrator → fake/OpenAI planner → non-root Docker sandbox → Git/file/build/test. Redis is an optional lease/rate-limit seam.

**APIs:** create/get session; get steps/diff; cancel/retry/PR; health. WebSocket topic `/topic/sessions/{id}`.

**Data:** `agent_sessions` stores owner, repo/task, monotonic state, plan, diff, summary, stop/cleanup, LLM tokens/calls, timestamps, optimistic version. `agent_steps` stores unique ordered tool attempts, inputs, outputs, status, and duration.

**Flow:** persist → allocate → clone → typed plan → validate and execute one call → audit → build/test → diff/summary → terminal state → cleanup.

**Security:** untrusted repo boundary; tool/command allowlists; normalized paths; no shell; time/step/resource limits; non-root/read-only container; restricted secrets; redaction; scoped GitHub App design. Production: OIDC plus gVisor/Kata/Firecracker and deny-by-default egress.

**Failures:** classify retryable/permanent; bounded exponential backoff; idempotency keys; unique step sequence; optimistic lock; durable checkpoints; REST recovery after WebSocket loss; cleanup reconciliation.

**Observability:** session/correlation IDs, durable audit, completed/failed counters, active sessions, LLM/tool/sandbox/build/test latency, tokens/cost, changed files, stop reason, cleanup outcome.

**Scaling:** stateless APIs, worker leases, tenant queues/quotas, sandbox-capacity autoscaling, object storage for logs, partitioned audit tables. Add outbox plus Kafka only for high-volume replayable multi-consumer events.

**Trade-offs:** modular monolith over premature services; Postgres truth over Redis; serialized steps over speculative concurrency; WebSockets for UX but not durability; Docker simplicity locally with explicit stronger-isolation path.
