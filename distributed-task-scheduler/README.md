# Distributed Task Scheduler

A Spring Boot distributed task scheduler with database-backed leader election, persistent job queue, retry backoff, and idempotent execution records.

## Stack

- Java 17
- Spring Boot 3.3.6
- Spring Web
- Spring Data JPA
- Flyway
- PostgreSQL
- Scheduled workers
- Docker Compose

## Endpoints

| Method | Path                     | Purpose                                    |
| ------ | ------------------------ | ------------------------------------------ |
| `POST` | `/api/jobs`              | Schedule a job                             |
| `GET`  | `/api/jobs`              | List jobs                                  |
| `GET`  | `/api/jobs/{id}`         | Read job status                            |
| `POST` | `/api/jobs/{id}/run-now` | Requeue a job immediately                  |
| `GET`  | `/api/leader`            | Inspect this instance's scheduler identity |
| `GET`  | `/actuator/health`       | Health check                               |

## Run

```powershell
docker compose up --build --scale api=2
```

The first API instance is published on `http://localhost:8085`.

## Smoke Test

Schedule a successful job:

```powershell
$job = Invoke-RestMethod -Method Post `
  -Uri http://localhost:8085/api/jobs `
  -ContentType 'application/json' `
  -Body '{
    "name": "daily-ledger-close",
    "payload": "close ledger for tenant A",
    "idempotencyKey": "ledger-close-2026-08-05",
    "maxAttempts": 3
  }'

$job
```

Schedule a job that retries:

```powershell
Invoke-RestMethod -Method Post `
  -Uri http://localhost:8085/api/jobs `
  -ContentType 'application/json' `
  -Body '{
    "name": "flaky-job",
    "payload": "fail until payload is changed",
    "idempotencyKey": "flaky-1",
    "maxAttempts": 2
  }'
```

Inspect jobs:

```powershell
Invoke-RestMethod http://localhost:8085/api/jobs
```

## Verify

```powershell
mvn clean verify
```
