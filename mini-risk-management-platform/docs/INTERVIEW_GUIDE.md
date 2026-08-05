# Senior Backend Interview Guide

Each answer is intentionally framed as what, why, and how. Practice answering out loud in two minutes, then drill deeper using the code and diagrams.

## Java And Spring

1. What is a JVM process?
Answer: What: a running Java virtual machine executing bytecode. Why: it gives portability, memory management, JIT optimization, and runtime tooling. How: Linux runs it as a process with threads, heap, file descriptors, sockets, and cgroup limits.

2. How does Java handle memory?
Answer: What: Java manages heap objects through garbage collection while also using native memory. Why: developers avoid manual free errors. How: GC traces reachable objects, reclaims unreachable ones, and the JVM also uses metaspace, thread stacks, direct buffers, and code cache.

3. Why do Java services need memory limits?
Answer: What: limits bound container memory consumption. Why: one service should not exhaust node memory. How: Kubernetes applies cgroup memory limits and kills the container if total memory exceeds the limit.

4. What causes long GC pauses?
Answer: What: the JVM pauses application threads for some collection phases. Why: it must maintain heap correctness. How: high allocation rate, oversized heaps, fragmentation, or old-generation pressure increase pause time.

5. What is dependency injection?
Answer: What: objects receive dependencies from a container instead of constructing them directly. Why: it improves testability and wiring consistency. How: Spring scans beans, resolves constructors, and creates an application context.

6. What does Spring Boot auto-configuration do?
Answer: What: it creates default beans based on classpath and properties. Why: it reduces repetitive setup. How: conditional configuration classes activate when required classes and settings are present.

7. How does a Spring Web request reach a controller?
Answer: What: HTTP is dispatched to a matching controller method. Why: application code should work with typed methods instead of raw sockets. How: embedded Tomcat accepts the socket, `DispatcherServlet` maps route and method, converters deserialize JSON, validation runs, then the controller executes.

8. Why keep controllers thin?
Answer: What: controllers should translate HTTP to application calls. Why: business logic becomes testable and transport-independent. How: this project puts workflow in `OrderApplicationService` and `RiskEvaluationService`.

9. What is Bean Validation?
Answer: What: declarative validation annotations such as `@NotBlank` and `@Positive`. Why: invalid input should be rejected before business logic. How: Spring invokes Jakarta Validation on `@Valid` request bodies.

10. Why set HTTP client timeouts?
Answer: What: connect and read timeouts bound downstream waits. Why: unbounded waits exhaust threads and cause cascading failure. How: `RestClient` uses a request factory configured with finite durations.

11. What is Actuator?
Answer: What: Spring Boot production endpoints for health, metrics, info, and diagnostics. Why: orchestrators and operators need machine-readable service state. How: this project exposes health and Prometheus metrics under `/actuator`.

12. What is a health probe?
Answer: What: an endpoint Kubernetes or Docker calls to classify a container. Why: automation needs a signal before routing or restarting. How: liveness decides restart; readiness decides traffic eligibility.

## Docker And Linux

13. What is a container?
Answer: What: an isolated process with its own filesystem, namespaces, and cgroup limits. Why: it packages runtime behavior consistently. How: Linux namespaces isolate views and cgroups constrain resources.

14. How is a container different from a VM?
Answer: What: a VM virtualizes hardware and runs a guest kernel; a container shares the host kernel. Why: containers start faster and use less memory. How: containers isolate processes with kernel primitives instead of booting a full OS.

15. What is an image layer?
Answer: What: an immutable filesystem diff. Why: layers make builds cacheable and storage efficient. How: OverlayFS merges layers into one view with a writable container layer on top.

16. Why use multi-stage Docker builds?
Answer: What: build dependencies and runtime dependencies live in separate stages. Why: runtime images become smaller and safer. How: Maven builds the jar in one image; the final image copies only the jar and JRE.

17. Why run containers as non-root?
Answer: What: the process runs with a non-root UID. Why: compromise has less privilege. How: Dockerfile creates an `app` user and the runtime stage uses `USER app`.

18. What is PID 1 in a container?
Answer: What: the first process in the container PID namespace. Why: it receives signals and must reap child processes. How: Docker starts `java -jar` as PID 1 unless an init process is used.

19. What is a namespace?
Answer: What: a scoped view of a Linux resource. Why: containers need isolated process trees, networks, mounts, and hostnames. How: Linux creates namespaces through syscalls such as `clone`, `unshare`, and `setns`.

20. What is a cgroup?
Answer: What: a Linux resource accounting and limit mechanism. Why: one process group must not consume unlimited CPU or memory. How: Docker and Kubernetes place container processes into cgroups with configured limits.

21. What is Docker bridge networking?
Answer: What: containers connect to a virtual Linux bridge. Why: containers need private IPs and service-to-service communication. How: Docker creates veth pairs and routes packets through the bridge with NAT as needed.

22. Why do Docker volumes exist?
Answer: What: external storage mounted into containers. Why: container writable layers are ephemeral. How: Docker mounts a named volume into paths such as `/var/lib/postgresql/data`.

23. What is `runc`?
Answer: What: a low-level OCI runtime. Why: a standard runtime is needed to create containers from specs. How: it sets namespaces, cgroups, mounts, capabilities, and then execs the process.

24. What is containerd?
Answer: What: a runtime manager for images, snapshots, and containers. Why: orchestrators need a stable container lifecycle API. How: Kubernetes talks to containerd through CRI, and containerd delegates process creation to `runc`.

## Kubernetes

25. What is a Pod?
Answer: What: the smallest Kubernetes deployable unit, containing one or more containers. Why: tightly coupled containers may share network and volumes. How: all containers in a Pod share an IP and can communicate over localhost.

26. What is a Deployment?
Answer: What: a controller for stateless replicated Pods. Why: teams need rollout, rollback, and self-healing. How: Deployment manages ReplicaSets, which maintain Pod count.

27. What is a Service?
Answer: What: stable virtual networking in front of Pods. Why: Pod IPs change. How: selectors find ready endpoints and kube-proxy or dataplane routes traffic.

28. What is Ingress?
Answer: What: HTTP routing from outside the cluster to Services. Why: many apps need shared external entry. How: an Ingress controller watches Ingress objects and configures proxy rules.

29. What is a ConfigMap?
Answer: What: non-secret configuration stored in Kubernetes. Why: config should change without rebuilding images. How: Pods consume ConfigMaps as env vars or mounted files.

30. What is a Secret?
Answer: What: a Kubernetes object for sensitive values. Why: credentials should not be stored in plain Deployment YAML. How: Pods reference Secret keys as env vars or files, though encryption at rest must be configured separately.

31. What is a PersistentVolume?
Answer: What: cluster storage resource. Why: stateful workloads need data outside Pod lifecycle. How: PVs bind to PVCs and are mounted into Pods.

32. What is a PersistentVolumeClaim?
Answer: What: a namespaced request for storage. Why: apps should request storage without knowing physical disks. How: Kubernetes binds a matching PV or provisions one dynamically.

33. What is an HPA?
Answer: What: HorizontalPodAutoscaler adjusts replica count. Why: stateless services should scale with load. How: HPA reads metrics such as CPU and updates Deployment replica count.

34. Why do Pods become Pending?
Answer: What: Kubernetes accepted the Pod but cannot run it yet. Why: scheduling or dependencies are unsatisfied. How: missing resources, unbound PVCs, taints, or image pull issues block startup.

35. What is CrashLoopBackOff?
Answer: What: repeated container crash with increasing restart delay. Why: Kubernetes avoids tight restart loops. How: kubelet restarts the container, observes repeated exits, then backs off.

36. What is readiness versus liveness?
Answer: What: readiness controls traffic; liveness controls restart. Why: not every temporary unready state requires killing the process. How: Kubernetes removes unready Pods from endpoints but restarts Pods failing liveness.

37. What are resource requests and limits?
Answer: What: requests reserve scheduling capacity; limits cap use. Why: clusters need predictable placement and isolation. How: scheduler uses requests; cgroups enforce limits.

38. Why can a Service have no endpoints?
Answer: What: no ready Pods match its selector. Why: selectors and readiness determine endpoints. How: EndpointSlice controller only includes matching ready Pods.

## PostgreSQL And Persistence

39. What is a relational database?
Answer: What: structured storage using tables, constraints, indexes, and SQL. Why: many business systems need durable consistency. How: PostgreSQL stores rows on disk and enforces transactions.

40. What is ACID?
Answer: What: atomicity, consistency, isolation, durability. Why: business operations must survive failure without partial corruption. How: transactions, WAL, locks, and MVCC implement these properties.

41. What is WAL?
Answer: What: write-ahead log. Why: recovery requires a durable record before data pages are flushed. How: PostgreSQL writes changes to WAL, then later checkpoints data pages.

42. What is MVCC?
Answer: What: multi-version concurrency control. Why: readers and writers should avoid blocking each other unnecessarily. How: PostgreSQL keeps row versions and transactions see snapshots.

43. Why use indexes?
Answer: What: data structures that speed lookup. Why: scanning every row is expensive. How: PostgreSQL can use B-tree indexes to find matching rows quickly.

44. What is a slow query?
Answer: What: a query whose execution time hurts user or system latency. Why: it consumes DB and app resources. How: missing indexes, bad plans, locks, or too much data can slow it.

45. What is a connection pool?
Answer: What: reusable DB connections. Why: opening connections per request is expensive. How: HikariCP keeps a bounded pool and requests borrow connections.

46. Why can too many connections hurt PostgreSQL?
Answer: What: each connection costs memory and scheduling overhead. Why: unbounded connections overload the database. How: many active sessions contend for CPU, locks, and buffers.

47. What is Flyway?
Answer: What: a database migration tool. Why: schema changes should be versioned and repeatable. How: it applies ordered SQL files and records them in `flyway_schema_history`.

48. Why avoid `ddl-auto=update` in production?
Answer: What: automatic schema mutation by ORM. Why: uncontrolled changes can corrupt production schema. How: migrations should be reviewed, ordered, and reversible by process.

49. What is schema ownership in microservices?
Answer: What: one service owns one schema. Why: direct table sharing couples deploys and breaks autonomy. How: services communicate through APIs or events.

50. Why store rejected orders?
Answer: What: rejected decisions remain in the order audit table. Why: financial systems need auditability. How: `order-service` saves both accepted and rejected orders with reason.

## Kafka And Messaging

51. What is Kafka?
Answer: What: a durable distributed event log. Why: services need decoupled asynchronous communication. How: producers append records to topic partitions and consumers read by offset.

52. What is a topic?
Answer: What: a named stream of records. Why: producers and consumers need a shared event channel. How: Kafka stores topic data split into partitions.

53. What is a partition?
Answer: What: an ordered append-only shard of a topic. Why: partitions provide scale and ordering boundaries. How: records with the same key can map to the same partition.

54. What is a consumer group?
Answer: What: a set of consumers sharing work. Why: consumers need scalable parallel processing. How: Kafka assigns partitions to group members.

55. What is consumer lag?
Answer: What: difference between latest offset and committed consumer offset. Why: lag measures processing delay. How: Kafka tracks offsets and exposes group position.

56. What is at-least-once delivery?
Answer: What: a record may be processed one or more times. Why: retry after failure is safer than silent loss. How: if processing succeeds but offset commit fails, the record can replay.

57. Why must consumers be idempotent?
Answer: What: repeated processing should not duplicate side effects. Why: Kafka can redeliver. How: `history-service` uses unique `order_id` to avoid double-counting.

58. What is a poison message?
Answer: What: a record that repeatedly fails processing. Why: it can block a partition. How: deserialization or validation errors occur at the same offset until handled.

59. What is a dead-letter topic?
Answer: What: a topic for failed records. Why: consumers should keep processing healthy records. How: after retry budget, publish the bad event and metadata to a DLQ.

60. Why use Kafka instead of direct calls for notifications?
Answer: What: notification is asynchronous side effect. Why: order placement should not wait for email. How: `notification-service` consumes `order-events` independently.

## Redis And Caching

61. What is Redis?
Answer: What: an in-memory data store. Why: it provides very low-latency reads and simple structures. How: Redis processes commands through an event loop and keeps data in memory.

62. What is cache-aside?
Answer: What: app checks cache, loads DB on miss, then writes cache. Why: the DB remains source of truth. How: `RiskLimitLookup` reads Redis, falls back to PostgreSQL, and sets TTL.

63. What is TTL?
Answer: What: time-to-live for cached data. Why: stale values should expire. How: Redis deletes or evicts keys after the configured duration.

64. What is cache invalidation?
Answer: What: removing or refreshing stale cache entries. Why: users need updated data after source-of-truth changes. How: update paths can delete keys or publish invalidation events.

65. What is a hot key?
Answer: What: one key receives disproportionate traffic. Why: it can overload one Redis shard or CPU. How: many requests hit the same key faster than Redis can serve comfortably.

66. What happens if Redis is down here?
Answer: What: risk-service logs cache errors and reads PostgreSQL. Why: cache should not be required for correctness. How: exceptions are caught in `RiskLimitLookup`.

67. What is cache stampede?
Answer: What: many clients miss the same key and hit DB together. Why: expired hot keys create DB spikes. How: single-flight, jittered TTLs, or pre-warming reduce it.

68. Why not store orders only in Redis?
Answer: What: Redis is not the authoritative order store here. Why: orders require durable audit history. How: PostgreSQL provides constraints, transactions, and durable recovery.

## Networking

69. What happens when a service calls `risk-service:8082`?
Answer: What: DNS resolves a service name to an IP, then TCP connects to port 8082. Why: services need stable names. How: Docker DNS or CoreDNS returns an address and the network dataplane routes packets.

70. What is TCP?
Answer: What: reliable byte-stream transport. Why: HTTP needs ordered delivery and retransmission. How: TCP uses handshakes, sequence numbers, acknowledgments, and congestion control.

71. What is DNS?
Answer: What: name to address resolution. Why: humans and services use stable names instead of changing IPs. How: clients query DNS servers and cache answers.

72. What is an HTTP gateway?
Answer: What: a public entry service that forwards or handles external requests. Why: clients should not know internal topology. How: `api-gateway` calls `order-service` and `history-service`.

73. What is a timeout?
Answer: What: maximum wait for an operation. Why: callers must preserve resources during dependency failure. How: HTTP clients abort connects or reads after configured durations.

74. What is a retry?
Answer: What: repeating a failed operation. Why: transient failures can recover. How: retries should be bounded, delayed, and only safe for idempotent or protected operations.

75. What is backpressure?
Answer: What: slowing producers when consumers or dependencies are saturated. Why: unbounded queues cause memory and latency failures. How: bounded queues, rate limits, and rejection protect systems.

76. What is a load balancer?
Answer: What: component distributing requests across backends. Why: capacity and availability improve with multiple replicas. How: Services, Ingress controllers, or external LBs choose endpoints.

## Microservices And System Design

77. Why split this into services?
Answer: What: each service owns a business capability. Why: teams, scaling, and failure boundaries differ. How: order, risk, history, and notification communicate by APIs and events.

78. What is service ownership?
Answer: What: one team/service owns behavior, data, and contracts. Why: unclear ownership causes unsafe changes. How: each service here owns its schema and API.

79. What is synchronous coupling?
Answer: What: caller waits for callee to respond. Why: it provides immediate answers but shares failure. How: order-service synchronously calls risk-service.

80. What is asynchronous coupling?
Answer: What: services communicate through events. Why: producers and consumers can evolve and recover independently. How: order-service publishes Kafka events.

81. Why fail closed in risk?
Answer: What: reject when required risk data is unavailable. Why: allowing unsafe financial orders is worse than rejecting safe ones. How: risk-service rejects if history-service cannot be read.

82. What is the outbox pattern?
Answer: What: store events in the same DB transaction as state changes. Why: it prevents DB commit plus message publish gaps. How: a separate publisher drains the outbox to Kafka.

83. What is eventual consistency?
Answer: What: different views become consistent after some delay. Why: async systems trade immediate consistency for decoupling. How: exposure updates after history-service consumes Kafka events.

84. What is idempotency?
Answer: What: same operation repeated produces the same effect. Why: retries and redelivery happen in distributed systems. How: history-service ignores duplicate `order_id`.

85. What is a bounded context?
Answer: What: a domain boundary with its own language and model. Why: different concepts should not be forced into one global model. How: order, risk, and history models are separate.

86. What is a contract?
Answer: What: agreed request, response, or event schema. Why: services evolve independently only when contracts are stable. How: shared records define REST and Kafka payloads.

87. How would you make this multi-region?
Answer: What: deploy services and data across regions. Why: reduce latency and survive region failure. How: partition clients by home region, replicate risk config, design Kafka and DB replication carefully, and define conflict rules.

88. How would you handle risk-limit updates?
Answer: What: authenticated admin path changes limits. Why: limits change during trading operations. How: write PostgreSQL transaction, publish invalidation event, delete Redis key, audit the change.

## Observability And Debugging

89. What are logs?
Answer: What: discrete event records. Why: they explain what happened. How: apps write stdout/stderr and platforms collect them.

90. What are metrics?
Answer: What: numeric time series. Why: they show trends, saturation, and alert signals. How: Micrometer exports Prometheus metrics from Actuator.

91. What are traces?
Answer: What: request path spans across services. Why: they show where distributed latency happens. How: instrumentation propagates trace IDs and records spans.

92. What is RED monitoring?
Answer: What: rate, errors, duration. Why: it captures user-facing service health. How: Prometheus queries aggregate HTTP counters and histograms.

93. What is USE monitoring?
Answer: What: utilization, saturation, errors. Why: it captures resource health. How: measure CPU, memory, disk, network, and queue saturation.

94. How do you debug a 503?
Answer: What: 503 means service unavailable at some boundary. Why: the cause could be gateway, downstream, DNS, or readiness. How: check gateway logs, downstream health, Service endpoints, and dependency logs.

95. How do you debug missing exposure updates?
Answer: What: accepted order did not appear in history. Why: Kafka publish, consumer lag, or DB write may have failed. How: check order DB, Kafka topic, consumer group lag, and history DB.

96. What is an alert runbook?
Answer: What: documented steps for an alert. Why: incidents need repeatable action under pressure. How: include symptom, commands, expected observations, fix, and escalation.

## Behavioral And Staff-Level

97. How do you explain a tradeoff to leadership?
Answer: What: describe options, risks, cost, and recommendation. Why: leadership needs decision clarity, not implementation detail only. How: tie impact to reliability, speed, money, and customer risk.

98. How do you handle an incident as the senior engineer?
Answer: What: stabilize, communicate, investigate, fix, and prevent recurrence. Why: coordination matters as much as technical skill. How: assign roles, post updates, preserve evidence, and write a postmortem.

99. How do you review a risky migration?
Answer: What: inspect schema impact, locks, data size, rollback, and compatibility. Why: DB changes can cause outages. How: test on realistic data, deploy expand-contract, and monitor.

100. How do you know this system is ready for production?
Answer: What: readiness is evidence, not opinion. Why: production has failures, load, security, and operational constraints. How: prove it with tests, load results, dashboards, alerts, runbooks, rollback, backups, and game days.

