# Technology Deep Dives

Use this as an interview preparation map. For each technology, be able to explain why it exists, what problem it solves, how it works, how it fails, and what tradeoffs you accepted in this project.

## Java 21

| Question | Answer |
| --- | --- |
| Why it exists | Java gives portable bytecode, strong tooling, mature libraries, runtime introspection, and predictable long-term support. |
| What problem it solves | Building long-lived server applications with concurrency, memory management, type safety, and operational tooling. |
| How it works internally | Source compiles to bytecode. The JVM loads classes, verifies bytecode, interprets initially, then JIT-compiles hot paths. The heap is managed by garbage collectors. |
| How Linux implements it | The JVM is a Linux process using syscalls for files, sockets, memory mapping, threads, timers, and signals. Java threads map to OS threads in the standard JVM model. |
| How Docker uses it | Docker runs the JVM as PID 1 inside a container namespace. Memory limits influence JVM heap sizing when container support is enabled. |
| How Kubernetes uses it | Kubernetes runs the Java process in a Pod, monitors probes, restarts failed containers, and applies CPU/memory limits through cgroups. |
| How this project uses it | Each service is a Java 21 Spring Boot app packaged as an executable jar. |
| Common interview questions | What is a JVM? What is GC? What happens during class loading? How do Java threads map to OS threads? |
| Production failures | OOMKilled, long GC pauses, thread pool exhaustion, blocked I/O, deadlocks, bad heap sizing. |
| Misconceptions | Java memory limit is not only heap. Native memory, metaspace, thread stacks, and direct buffers also count. |
| Best practices | Set resource limits, monitor heap and GC, use timeouts, expose Actuator, avoid unbounded executors. |
| Tradeoffs | Java has strong observability and ecosystem support but uses more memory than smaller runtimes. |

## Spring Boot And Spring Web

| Question | Answer |
| --- | --- |
| Why it exists | Reduces boilerplate for production Java services and provides consistent web, config, metrics, validation, and dependency wiring. |
| What problem it solves | Turning Java code into deployable HTTP services with sane defaults. |
| How it works internally | Auto-configuration creates beans based on classpath and properties. Controllers are mapped by `DispatcherServlet`. |
| How Linux implements it | The embedded server binds a TCP socket and worker threads process accepted connections. |
| How Docker uses it | The container exposes the HTTP port and health checks call Actuator endpoints. |
| How Kubernetes uses it | Readiness and liveness probes call Actuator endpoints before routing traffic. |
| How this project uses it | Controllers define API boundaries; services hold business logic; Actuator exposes health and Prometheus metrics. |
| Common interview questions | What is dependency injection? What is `DispatcherServlet`? How does validation work? |
| Production failures | Slow endpoints, missing timeouts, dependency injection cycles, broken config, oversized thread pools. |
| Misconceptions | Spring Boot is not magic. It is conditional bean creation and convention-based wiring. |
| Best practices | Keep controllers thin, put business logic in services, externalize config, set timeouts. |
| Tradeoffs | Fast development and rich ecosystem at cost of startup time and abstraction layers. |

## Spring Data JPA And Flyway

| Question | Answer |
| --- | --- |
| Why it exists | JPA maps objects to relational tables; Flyway makes schema changes explicit and repeatable. |
| What problem it solves | Persistence without hand-writing every SQL statement, plus safe schema evolution. |
| How it works internally | Hibernate tracks entities in a persistence context and flushes SQL. Flyway scans migrations and stores applied versions in `flyway_schema_history`. |
| How Linux implements it | JDBC uses TCP sockets to PostgreSQL and file/network syscalls through the JVM. |
| How Docker uses it | Services connect to the `postgres` container over the Compose bridge network. |
| How Kubernetes uses it | Services connect to the `postgres` Service DNS name; readiness fails when DB is unavailable. |
| How this project uses it | Each owning service has its own migrations and JPA repositories. |
| Common interview questions | What is a transaction? What is N+1? How do migrations roll forward? |
| Production failures | Slow queries, lock contention, pool exhaustion, bad migration, schema drift. |
| Misconceptions | JPA does not remove the need to understand SQL. |
| Best practices | Keep migrations small, index access paths, validate schema, monitor connection pools. |
| Tradeoffs | JPA improves productivity but can hide inefficient SQL. |

## PostgreSQL

| Question | Answer |
| --- | --- |
| Why it exists | Durable relational storage with transactions, indexes, constraints, and SQL querying. |
| What problem it solves | Correct storage for orders, risk limits, and exposure audit history. |
| How it works internally | Uses processes, shared buffers, WAL, checkpoints, MVCC, indexes, and background workers. |
| How Linux implements it | Files on disk, page cache, fsync, sockets, process scheduling, and memory management. |
| How Docker uses it | Data files live on a named volume mounted into `/var/lib/postgresql/data`. |
| How Kubernetes uses it | Data files live on a PVC. In production, use StatefulSet and managed storage. |
| How Java interacts with it | JDBC connections from HikariCP execute SQL generated by Hibernate or Flyway. |
| Common interview questions | What is WAL? What is MVCC? How do indexes work? Why use transactions? |
| Production failures | Disk full, long transactions, deadlocks, connection exhaustion, replication lag, corrupted volumes. |
| Misconceptions | More indexes always help. They speed reads but slow writes and consume space. |
| Best practices | Backups, migrations, indexes, connection pool sizing, query plans, slow query logs. |
| Tradeoffs | Strong consistency and SQL power at cost of vertical scaling pressure and operational care. |

## Kafka

| Question | Answer |
| --- | --- |
| Why it exists | Durable event log for decoupling producers and consumers. |
| What problem it solves | `order-service` should not synchronously call every side-effect service. |
| How it works internally | Producers append records to topic partitions. Consumers track offsets in consumer groups. Brokers store segment files on disk. |
| How Linux implements it | Kafka uses TCP sockets, file I/O, page cache, disk flushes, threads, and JVM memory. |
| How Docker uses it | Single broker runs in KRaft mode in one container with a named volume. |
| How Kubernetes uses it | A real cluster usually uses StatefulSets, stable identities, PVCs, and multiple brokers. |
| How Java interacts with it | Spring Kafka serializes `OrderEvent` to JSON and consumers deserialize into records. |
| Common interview questions | What is a partition? What is consumer lag? What does at-least-once mean? |
| Production failures | Broker unavailable, ISR shrink, consumer lag, poison messages, rebalances, disk full. |
| Misconceptions | Kafka is not a queue only. It is an append-only distributed log with replay. |
| Best practices | Idempotent consumers, partitioning strategy, DLQs, schema compatibility, lag alerts. |
| Tradeoffs | Excellent decoupling and replay, but operationally more complex than direct HTTP. |

## Redis

| Question | Answer |
| --- | --- |
| Why it exists | Fast in-memory data store for caching and simple data structures. |
| What problem it solves | Risk limits are read frequently and change less often than orders. |
| How it works internally | Single-threaded command execution over an event loop, in-memory structures, optional persistence. |
| How Linux implements it | TCP sockets, epoll, memory allocation, optional append-only file writes. |
| How Docker uses it | Redis data is mounted to `/data` with append-only persistence enabled. |
| How Kubernetes uses it | Redis Pod mounts a PVC and exposes a ClusterIP Service. |
| How Java interacts with it | `StringRedisTemplate` reads and writes JSON risk-limit snapshots with TTL. |
| Common interview questions | Cache-aside vs write-through? What is TTL? What is cache stampede? |
| Production failures | Eviction, stale values, hot keys, connection storms, memory fragmentation. |
| Misconceptions | Cache is not source of truth. PostgreSQL remains source of truth here. |
| Best practices | TTLs, fallback to DB, monitor hit ratio, protect hot keys, clear invalidation path. |
| Tradeoffs | Low latency at cost of staleness and another dependency. |

## Docker

| Question | Answer |
| --- | --- |
| Why it exists | Package and isolate application processes reproducibly. |
| What problem it solves | Environment drift and dependency mismatch. |
| How it works internally | Images are layered filesystems. Containers are Linux processes with namespaces and cgroups. |
| How Linux implements it | clone/unshare/setns, cgroups, OverlayFS, bridge networking, iptables or nftables. |
| How Docker uses it | Docker daemon builds images, creates containers, connects networks, mounts volumes, and starts `runc`. |
| How Kubernetes uses it | Kubernetes does not need Docker specifically; it uses CRI-compatible runtimes such as containerd. |
| How Java interacts with it | Java sees a constrained filesystem, process namespace, network namespace, and cgroup limits. |
| Common interview questions | Container vs VM? What is an image layer? What is PID 1? |
| Production failures | Bad image, wrong env, missing port, OOM, DNS issue, volume issue. |
| Misconceptions | Containers are not security boundaries as strong as VMs by default. |
| Best practices | Small images, non-root users, health checks, pinned tags, logs to stdout. |
| Tradeoffs | Faster and lighter than VMs, but shares host kernel. |

## Kubernetes

| Question | Answer |
| --- | --- |
| Why it exists | Run containers reliably across a cluster. |
| What problem it solves | Scheduling, restarts, rollouts, service discovery, config, secrets, scaling. |
| How it works internally | API server stores desired state; controllers reconcile; scheduler places Pods; kubelet starts containers. |
| How Linux implements it | Kubelet and runtime create Linux namespaces, cgroups, mounts, and network interfaces. |
| How Docker uses it | Modern Kubernetes usually talks to containerd, not the Docker daemon. Docker images still use OCI format. |
| How Java interacts with it | Java processes receive env vars, listen on ports, answer probes, and respect resource limits. |
| Common interview questions | Pod vs container? Service vs Ingress? Deployment vs StatefulSet? |
| Production failures | CrashLoopBackOff, ImagePullBackOff, OOMKilled, bad Service selectors, DNS failure, probe failure. |
| Misconceptions | Kubernetes does not fix bad application behavior. It restarts and routes based on your configuration. |
| Best practices | Requests/limits, probes, rollouts, secrets, network policies, observability. |
| Tradeoffs | Powerful self-healing platform at cost of operational complexity. |

## Prometheus And Grafana

| Question | Answer |
| --- | --- |
| Why it exists | Metrics let engineers see system health over time. |
| What problem it solves | Without metrics, debugging depends only on logs and guesses. |
| How it works internally | Prometheus scrapes HTTP endpoints and stores time series. Grafana queries and visualizes them. |
| How Linux implements it | TCP scraping, local disk time-series storage, process scheduling. |
| How Docker uses it | Prometheus and Grafana run as containers with mounted config and data volumes. |
| How Kubernetes uses it | Prometheus discovers or scrapes Services and Pods; dashboards show service behavior. |
| How Java interacts with it | Spring Actuator and Micrometer expose `/actuator/prometheus`. |
| Common interview questions | Metrics vs logs vs traces? What is cardinality? What is an SLI? |
| Production failures | Missing scrape target, high cardinality, stale dashboards, noisy alerts. |
| Misconceptions | Dashboards are not alerts. Alerts need thresholds and action. |
| Best practices | RED/USE metrics, low cardinality labels, alert runbooks, owner per dashboard. |
| Tradeoffs | Metrics are cheap and aggregate well, but lack per-request detail. |

## GitHub Actions

| Question | Answer |
| --- | --- |
| Why it exists | Automates checks before code reaches production branches. |
| What problem it solves | Human reviewers should not manually verify every build and test. |
| How it works internally | Hosted runners execute workflow steps in clean environments. |
| How Linux implements it | Each job runs as processes on a runner VM with filesystem and network access. |
| How Docker uses it | Docker build steps validate container image build contexts. |
| How Kubernetes uses it | CI can later publish images and run `kubectl` or GitOps updates. |
| How Java interacts with it | Maven runs tests under Java 21 from `actions/setup-java`. |
| Common interview questions | What belongs in CI? What is the difference between CI and CD? |
| Production failures | Flaky tests, missing secrets, slow builds, unpinned actions, cache poisoning. |
| Misconceptions | Passing CI does not prove production readiness. |
| Best practices | Fast tests first, deterministic builds, least-privilege secrets, clear failure output. |
| Tradeoffs | More checks catch more defects but slow delivery if not designed well. |

