# Interview guide and revision sheet

## 30 seconds

I built a secure AI risk analyst where the LLM is deliberately treated as an untrusted planner. Deterministic code controls prompt and document inspection, policy, read-only tools, output DLP, human approval, and durable audit. I then attacked those boundaries with prompt injection, a malicious document, an unauthorized write-tool request, and secret exfiltration; every attack is blocked and audited by executable tests.

## 2 minutes

Walk through ingestion, explainable signals, case creation, tool evidence, cited RAG, structured output, optimistic locking, approval, audit, and outbox. Emphasize why rules remain separate from AI and why a modular monolith is the right starting point.

## 10-minute system design

1. Requirements and safety invariants (1 min)
2. Sync scoring and case creation path (2 min)
3. Data model/source-of-truth and outbox (2 min)
4. RAG/tool calling and injection boundaries (2 min)
5. Human approval and auditability (1 min)
6. Reliability, observability, scaling, AWS evolution (2 min)

## Key decisions

- Modular monolith first: transactional consistency and fast iteration; extract only proven scale/ownership boundaries.
- PostgreSQL truth, Redis acceleration: degraded cache cannot corrupt decisions.
- Outbox over dual write: database state and intent to publish commit together.
- Mocked LLM locally: reproducible demo without credentials or cost.
- Retrieval abstraction: local lexical ranking is deterministic; production swaps in pgvector while retaining citations.

## Common questions

- **Why Kafka?** Case-created, approval, notification, analytics, and retraining are asynchronous; authorization scoring stays synchronous.
- **Exactly once?** I promise at-least-once plus idempotent effects, not magical distributed exactly-once behavior.
- **How do you stop hallucinated actions?** The model returns a schema-shaped recommendation only. No write tool is exposed, and RBAC approval is a separate endpoint.
- **What happens when a blocked request rolls back?** Security audit writes use an independent transaction, so the denial record survives the failed business transaction.
- **Is regex enough for prompt injection?** No. It is a deterministic demonstration control. Production adds classifiers, context isolation, egress denial, scoped credentials, policy-as-code, and continuously expanded adversarial evals.
- **How do metrics stay honest?** Model quality needs delayed ground truth; unlabeled cases are excluded.
- **How would it scale?** Partition by account/card, scale stateless API pods, keep event consumers idempotent, use RDS read replicas only for read-heavy evidence.
- **Why not microservices?** Current boundaries need shared transactions and one team; premature distribution adds failure modes without business value.

## Four honest resume bullets

- Built a Java 21/Spring Boot modular-monolith fraud investigation demo combining explainable rules, case workflows, cited policy retrieval, and schema-shaped AI recommendations.
- Enforced least-privilege AI through input/document guards, an explicit read-tool allowlist, output DLP, durable denial audits, and mandatory senior approval for sensitive actions.
- Designed PostgreSQL source-of-truth persistence with optimistic locking, idempotency keys, transactional outbox records, and at-least-once Kafka semantics.
- Delivered a React analyst console, deterministic mock-LLM mode, end-to-end RBAC test, Prometheus endpoint, Docker Compose, Kubernetes starter, CI, and AWS ECR Terraform.

## One-page revision

Invariants: rules are deterministic; LLM has no DB or write access; irreversible action requires a human; PostgreSQL is truth. Thresholds: low <40, medium 40–69, high ≥70. Reliability: idempotency + outbox + optimistic locking + DLT. AI: retrieve, cite, tool-allowlist, mask, validate, audit. Scaling: stateless API, keyed partitions, distributed cache only for acceleration. Demo: ingest risky → signals → investigate → citations → analyst denied approval → senior approves → audit.
