# Interview Guide

## Two-minute explanation

I built the smallest production-shaped cloud coding agent. A React client creates a durable session in a Spring Boot modular monolith. The orchestrator allocates a constrained non-root Docker workspace, clones an untrusted repository, asks a provider-neutral LLM for a typed plan, and executes one validated allowlisted tool call at a time. Every state transition and tool result is persisted in PostgreSQL before being streamed over WebSockets, so disconnects do not lose truth. The system bounds steps, time, commands, paths, tokens, and cleanup. A deterministic fake provider makes tests repeatable; an OpenAI adapter provides a real path. The design deliberately avoids Kafka and microservices until workload or independent event consumers justify them. Its honest production seams are OIDC authorization, GitHub App credentials, a remote sandbox worker, network isolation, and checkpoint reconciliation.

## Five-minute walkthrough

Start at `SessionController`: REST creates, reads, cancels, and retries sessions. `AgentSession` owns the monotonic state machine and optimistic version. `SessionService` is the workflow: allocate, plan, execute, validate, persist, emit, clean up. `LlmClient` isolates providers; fake output is deterministic and OpenAI output must parse into typed actions. `ToolExecutor` applies an allowlist, path normalization, no-shell command parsing, timeouts, redaction, and audit-friendly results. `SandboxManager` creates resource-limited UID-10001 containers and never places the Docker socket or LLM secrets inside them. Flyway creates durable sessions and uniquely sequenced steps. STOMP provides low-latency progress but REST reconstructs state after disconnects. Compose makes the vertical slice runnable; Kubernetes manifests teach the production shape without insecure Docker-in-Docker.

## Decisions and trade-offs

- Modular monolith: transactional clarity and fast comprehension; split workers only when sandbox load needs independent scaling.
- PostgreSQL: workflow truth, constraints, and queries. Redis: leases/rate limits later, not truth.
- WebSockets: bidirectional cancellation-ready live UX; polling REST remains recovery.
- Orchestrator outside sandbox: policy and credentials cannot be rewritten by repository content; it creates a privileged control-plane boundary.
- One tool at a time: easy auditing, cancellation, budgets, and recovery; lower parallelism.
- Docker locally: simple and demonstrable; production isolation should use gVisor, Kata, or Firecracker.
- No Kafka initially: no independent replay consumers or throughput need. Add transactional outbox plus Kafka when those appear.

## Failures and scaling

Duplicate creates need an API idempotency key mapped to one session. Steps use unique sequence numbers; remote effects need their own idempotency keys. Retry only classified transient clone, provider, or infrastructure errors with exponential backoff and jitter. Never blindly retry writes, commits, or PR creation. Checkpoints plus optimistic locks support restart recovery. At scale, stateless API replicas persist jobs, workers lease sessions, per-tenant queues enforce fairness, sandbox capacity autoscaling limits concurrency, and object storage holds large logs/diffs. Partition tables and archive audit history before considering a different workflow store.

Common production failures: expired installation tokens, clone throttling, prompt injection, malicious builds, dependency supply-chain attacks, LLM timeouts, malformed tool calls, disk exhaustion, zombie containers, database failover, duplicate delivery, reconnect storms, and cost runaway.

## Interview questions

1. **Why WebSockets?** Low-latency progress and future interactive approvals; REST is recovery truth.
2. **Why not SSE?** SSE is simpler for one-way logs, but cancellation/approval interaction benefits from a bidirectional channel.
3. **Why PostgreSQL?** Transactions, constraints, optimistic locking, and operational maturity.
4. **Why Redis?** Distributed leases, rate limits, and ephemeral presence—not durable session state.
5. **Why no Kafka?** One workflow owner and no replaying independent consumers yet.
6. **Exactly once?** Impossible across these effects; use idempotency, dedupe, and checkpoints.
7. **How does retry work?** Classify, cap attempts, back off with jitter, and record every attempt.
8. **How do you stop prompt injection?** Treat repository content as data and enforce policy outside the model.
9. **Why outside the sandbox?** The control plane protects credentials, budgets, and policy.
10. **Path safety?** Normalize, resolve under the workspace root, and reject escapes.
11. **Command safety?** Allowlisted executables, no shell, typed arguments, timeout, limited environment.
12. **Secret safety?** Short-lived scoped credentials, no ambient env, redaction, immutable audit.
13. **Container escape?** Docker is not a perfect boundary; use hardened kernels/microVMs in production.
14. **Cancellation?** Cooperative checks between steps, then forced process/container termination.
15. **WebSocket loss?** Reconnect and fetch session/steps from PostgreSQL.
16. **Backend crash?** A reconciler leases non-terminal checkpoints and resumes safe work.
17. **Duplicate request?** Client idempotency key with a unique database constraint.
18. **LLM malformed output?** Schema parsing, bounded repair retry, then a diagnosable failure.
19. **Cost control?** Token accounting, model policy, context bounds, step cap, tenant budget.
20. **Large logs?** Chunk to object storage and persist metadata/checksums.
21. **Many tenants?** Tenant-aware auth, quotas, queues, encryption, and noisy-neighbor controls.
22. **Many sessions?** Separate worker pool and autoscale on leased backlog and sandbox capacity.
23. **Database bottleneck?** Index, batch events, partition audit tables, replicas for reads.
24. **Tool parallelism?** Only for proven independent read steps; writes remain serialized.
25. **GitHub security?** App installation tokens scoped to one repository and short lifetime.
26. **PR safety?** Separate explicit user action, branch protection, signed audit, no implicit publish.
27. **Build failure?** Preserve output and mark validation outcome honestly.
28. **Cleanup failure?** Persist it, retry out of band, alert on leaked resources.
29. **Observability?** Correlation/session IDs, state/step audit, latency, failure, cost, cleanup metrics.
30. **What next?** Production auth and remote hardened worker before smarter agent behavior.

## Ten takeaways

1. The model proposes; deterministic policy decides.
2. Durable state precedes live delivery.
3. Tool calls are a security API, not arbitrary shell text.
4. At-least-once effects demand idempotency.
5. Monotonic states simplify recovery.
6. Isolation quality matters more than UI polish.
7. Fake providers make agent tests deterministic.
8. Cancellation and cleanup are first-class workflow states.
9. A modular monolith is often the best first distributed-system boundary.
10. State honest gaps before proposing scale.

## Honest resume bullets

- Built a Java 21/Spring Boot and React coding-agent vertical slice with durable PostgreSQL workflows, typed LLM planning, audited tools, and live WebSocket progress.
- Implemented disposable non-root Docker workspaces with resource limits, path/command validation, timeouts, secret redaction, and cleanup reporting.
- Designed provider-neutral fake/OpenAI adapters, monotonic state transitions, optimistic locking, metrics, Compose, Kubernetes learning manifests, and CI tests.
