# Distributed Task Scheduler Interview Guide

## Two-Minute Pitch

This service schedules jobs into PostgreSQL, elects one active scheduler through a database lock, executes due work through workers, records attempts, and retries failed jobs with bounded backoff. It is a focused project for explaining reliability without needing a full external orchestrator.

## What To Emphasize

- Jobs are durable records, not in-memory timers.
- Leader election prevents multiple scheduler instances from claiming the same work.
- Idempotency keys prevent duplicate logical jobs.
- Execution records make retries auditable.
- `run-now` is an operator escape hatch for demos and recovery.

## Request Flow

1. Client schedules a job through `POST /api/jobs`.
2. The service persists job metadata, status, due time, and idempotency key.
3. Each instance competes for a database-backed scheduler lock.
4. The leader finds due jobs and dispatches workers.
5. Each attempt is recorded and the job moves to succeeded, retrying, or failed.

## Tradeoffs

| Decision | Benefit | Cost |
| --- | --- | --- |
| Database-backed queue | Simple durable scheduler | Less throughput than a broker |
| Database lock leader election | Easy local multi-instance demo | Requires careful lock timeout tuning |
| Scheduled polling | Understandable control loop | Jobs may run slightly after due time |
| Attempt records | Clear debugging and audit | More writes per job |

## FAQ

Q: Why not use Kafka for jobs?
A: A database queue is enough for delayed scheduling and is easier to inspect. Kafka is better for streams and high-throughput event processing.

Q: What makes this distributed?
A: Multiple app instances can run, but only the lock holder should act as scheduler leader at a time.

Q: What would you add next?
A: advisory locks, worker leases, cron expressions, poison-job quarantine, metrics, and idempotent business handlers.
