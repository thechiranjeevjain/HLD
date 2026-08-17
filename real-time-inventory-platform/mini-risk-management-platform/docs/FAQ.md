# Mini Risk Management Platform FAQ

## Architecture

Q: Why split order, risk, history, and notification services?
A: Each service owns a distinct responsibility. Order owns workflow, risk owns decisioning, history owns exposure/audit state, and notification owns side effects.

Q: Why have an API gateway?
A: It gives clients one entry point and lets internal services evolve without exposing every service directly.

Q: Why use Kafka?
A: Kafka decouples event producers from consumers and makes order events replayable for downstream workflows.

Q: Why use Redis?
A: Redis is useful for fast ephemeral state such as cache, rate limits, or short-lived lookup state. It should not replace durable truth.

## Reliability

Q: What is fail-closed risk behavior?
A: If risk state or dependencies are unsafe, the platform should reject rather than accept an order that might breach limits.

Q: What should be idempotent?
A: order submission, event publishing, event consumption, and notification side effects should all have dedupe boundaries.

Q: What breaks first at scale?
A: synchronous downstream calls, database contention, Kafka lag, hot account/symbol partitions, and high-cardinality metrics.

## Operations

Q: What do Docker health checks prove?
A: They prove a container can answer the configured health endpoint. They do not prove the full business workflow is correct.

Q: What do Kubernetes readiness probes do?
A: They decide whether a pod should receive traffic.

Q: What would you add next?
A: distributed tracing, alert rules, load tests, chaos drills, secrets rotation, and production-grade dashboards.
