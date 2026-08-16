# Production Failure Runbook and Labs

Use `docker compose` locally. Establish a healthy baseline from Prometheus, application logs, PostgreSQL activity, Kafka consumer lag, and the outbox backlog before injecting faults.

| Failure                      | Inject                                                         | Detect and diagnose                                                    | Mitigate and prevent                                                                                  |
| ---------------------------- | -------------------------------------------------------------- | ---------------------------------------------------------------------- | ----------------------------------------------------------------------------------------------------- |
| PostgreSQL unavailable       | `docker compose stop postgres`                                 | readiness fails; connection timeout/errors; DB metrics corroborate     | fail fast, restore DB, do not retry writes blindly; Multi-AZ, backups and restore drills              |
| Redis unavailable            | `docker compose stop redis`                                    | cache errors, hit rate drops, DB traffic/latency rises                 | treat cache as optional with bounded timeout/error handler; capacity-plan DB and use circuit breaking |
| Kafka unavailable            | `docker compose stop kafka`                                    | publish errors and oldest-outbox age rise while orders still commit    | restore broker and drain backlog; bound retries, alert on age, size outbox storage                    |
| Duplicate event              | replay a payload with same `eventId`                           | duplicate counter/log; one `processed_events` row                      | unique inbox key and transaction with business effect                                                 |
| Out-of-order event           | publish later state before earlier state                       | invalid transition/version metric                                      | key by aggregate, include sequence/version, reject stale events and reconcile                         |
| Slow SQL                     | run `SELECT pg_sleep(3)` or remove an index in a disposable DB | p95 rises, pool active reaches max, `pg_stat_activity`, slow query log | `EXPLAIN ANALYZE`, index/query fix, statement timeout; never mask with an enormous pool               |
| Pool exhaustion              | lower pool size and issue concurrent slow requests             | pending connection gauge, thread stacks waiting on Hikari              | cancel slow queries, shed load, right-size total connections, backpressure                            |
| Pod crash                    | `kubectl delete pod ...`                                       | restart count and availability; requests should continue               | multiple replicas, PDB, readiness, graceful shutdown                                                  |
| High latency/network timeout | add latency with Toxiproxy or `tc netem` in a lab              | dependency timer separates app from network time                       | explicit connect/read/deadline budgets; jittered retry only for safe operations                       |
| Memory leak                  | capture JFR/heap dump from a controlled load test              | old-gen trend, GC pause, OOMKilled                                     | dominator analysis, bound collections/caches, set memory limit and headroom                           |
| Thread starvation            | block request threads with slow downstream calls               | busy threads, queue depth, thread dump                                 | timeouts, bulkheads, bounded pools; prefer async only when it reduces blocking                        |
| Bad deployment               | deploy invalid config/image                                    | rollout stalls, readiness fails, error-budget burn                     | immutable images, canary/progressive delivery, automatic rollback, config validation                  |

## Debugging order

1. Confirm user impact and time window; freeze risky changes.
2. Check recent deploy/config/traffic changes.
3. Use metrics to identify scope and saturated resource.
4. Follow one trace/request ID, then inspect targeted logs.
5. Mitigate first: rollback, shed load, fail over, or disable a feature.
6. Preserve evidence, write a blameless timeline, and add a prevention/detection action.

Linux tools map to OS concepts: `top`/`pidstat` reveal scheduling and CPU; `free` and cgroup files reveal memory limits; `ss` shows sockets and queues; `dig` checks DNS; `curl -v` exposes TCP/TLS/HTTP timing; `jcmd`, JFR, and thread dumps expose JVM allocation, locks, and blocked threads. Always correlate clocks and request IDs.
