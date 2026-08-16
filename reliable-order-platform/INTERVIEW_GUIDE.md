# Senior Interview Guide

## Requirements and sizing

Functional: authenticate users, create an order exactly once from the user's perspective, read authorized orders, emit an event, process it asynchronously, and retain an audit trail. Non-functional: p95 create latency below 300 ms under normal load, 99.9% availability, no acknowledged order loss, recovery point near zero for committed PostgreSQL state, and async acceptance within 30 seconds.

Start an interview with estimates: peak requests/second, read/write ratio, event size, retention, and regional requirements. At 1,000 creates/s and 1 KB/event, Kafka receives roughly 86 GB/day before replication. These estimates choose partitions, storage, connection pools, and cost; they are not decoration.

## Why each technology exists

| Choice           | Problem solved                                      | Alternative and trade-off                                            | Do not use when                                              |
| ---------------- | --------------------------------------------------- | -------------------------------------------------------------------- | ------------------------------------------------------------ |
| PostgreSQL       | constraints, transactions, durable relational state | DynamoDB scales keys simply but shifts joins/constraints to code     | ephemeral or append-only data needs no relational invariants |
| Redis            | removes repeated read latency/load                  | local Caffeine is faster but incoherent across pods                  | hit rate is low or stale data is unsafe                      |
| Kafka            | durable fan-out, replay, partition ordering         | SQS is operationally simpler but has weaker stream/replay semantics  | synchronous work is required to complete the request         |
| Outbox           | atomically couples state and intent to publish      | CDC/Debezium lowers polling but adds platform complexity             | no event must follow a database commit                       |
| Modular monolith | one deployment and transaction boundary             | microservices give independent scaling/ownership at operational cost | teams/domains truly need independent release and scaling     |
| Kubernetes       | scheduling, rollout, health, self-healing           | ECS is simpler on AWS                                                | a small team has no platform need or expertise               |
| Terraform        | reviewable repeatable infrastructure                | CloudFormation is AWS-native; Pulumi uses general languages          | one-off experiments where teardown is immediate              |

## Consistency and failure semantics

The order transaction is strongly consistent inside PostgreSQL. Cache and fulfillment state are eventually consistent. Kafka preserves order only within a partition, so the producer keys by order ID. Duplicates remain possible when publication succeeds but the database mark fails; consumers deduplicate by event ID. An inbox record and business update share one transaction.

The current poller holds a database transaction while waiting for Kafka. That is easy to reason about but can hold locks during broker latency. At higher scale, use `FOR UPDATE SKIP LOCKED`, claim rows briefly, publish outside the transaction, and use leases/retries—or CDC. Watch outbox age, not only row count.

## Security

OIDC separates credential handling from business code. Validate issuer, signature, expiry, audience, and scopes/roles. Authorization is both route-level and object-level. Apply TLS everywhere, least-privilege IAM, network policies/security groups, encrypted storage, secret rotation, dependency/image scanning, input limits, rate limiting, and redaction. Audit logs must be append-oriented and access-controlled; ordinary application logs are not an audit system.

Common mistakes: trusting claims without validating issuer/audience, using predictable IDs as authorization, logging tokens, storing production secrets in Kubernetes YAML, disabling TLS to fix connectivity, and granting the pod broad AWS permissions.

## Performance and scaling

Scale stateless API pods horizontally only after locating the bottleneck. HPA on CPU misses I/O saturation; add request concurrency/latency and Kafka lag metrics. Database capacity is bounded by CPU, IOPS, locks, and connections. A 10-connection pool across 20 pods means 200 database connections. Tune the total, set timeouts, index measured queries, use `EXPLAIN ANALYZE`, and avoid N+1 queries.

Kafka partition count caps consumer-group parallelism. More partitions increase metadata, open files, and rebalancing cost. Redis reduces read load but can create stampedes; use bounded TTL jitter, single-flight, and prewarming only for measured hot keys.

## Observability and SRE framing

Use the four golden signals: latency, traffic, errors, and saturation. Add business signals: orders created, rejected, outbox age, consumer lag, and time-to-accept. Structured logs carry request/trace ID, actor, order ID, outcome, and latency without secrets. Metrics answer “is this widespread?”; traces answer “where is time spent?”; logs answer “what happened here?”

Suggested SLO: 99.9% of valid create requests succeed monthly and p95 is below 300 ms, excluding explicit client errors. Alert on multi-window error-budget burn rather than every transient spike.

## Strong interview answers

**Why not call Kafka directly in the request?** “A database commit and Kafka publish cannot form one ordinary atomic transaction. Either ordering creates a loss window. I persist the state and outbox intent together, then publish asynchronously. Delivery is at least once, so consumers are idempotent.”

**What happens when Redis fails?** “Redis is an optimization. Reads fall back to PostgreSQL, latency and database load rise, and we alert on cache errors plus DB saturation. We use short connect timeouts and avoid retries that amplify an outage.”

**Liveness versus readiness?** “Liveness asks whether restarting may help; readiness asks whether this instance should receive traffic. A database outage should normally make a pod unready, not trigger an endless restart storm. Startup probes protect slow JVM initialization.”

**When split a service?** “When an independently owned domain needs distinct scaling, availability, data lifecycle, or release cadence—and the operational cost is justified. I do not split merely because classes can be grouped.”

## Practice questions (answer before reading references)

1. Where can a duplicate event arise, and why is the database constraint still required?
2. How would you evolve an event schema without breaking lagging consumers?
3. What metrics distinguish a slow database from thread-pool starvation?
4. How do rolling deployments interact with schema migrations?
5. Which components need multi-AZ, and what failure does each protect against?
6. At what traffic or organizational threshold would you replace polling with CDC?
7. How would you make the cache resilient without hiding a Redis outage?
8. Explain the network path from DNS lookup to Spring controller.

For self-review, structure each answer as: requirement/assumption → design → failure behavior → observable evidence → trade-off. A senior answer names uncertainty and the metric or experiment that would resolve it.
