# Distributed Task Scheduler Demo Script

## Start

```powershell
cd G:\TechStudyNotes\SystemDesignProjects\distributed-task-scheduler
docker compose up --build --scale api=2
```

API base URL: `http://localhost:8085`.

## Walkthrough

1. Check which instance is leader.

```powershell
Invoke-RestMethod http://localhost:8085/api/leader
```

2. Schedule a successful job.

```powershell
$job = Invoke-RestMethod -Method Post `
  -Uri http://localhost:8085/api/jobs `
  -ContentType 'application/json' `
  -Body '{"name":"daily-ledger-close","payload":"close ledger for tenant A","idempotencyKey":"ledger-close-demo","maxAttempts":3}'
```

3. Schedule a retrying job.

```powershell
Invoke-RestMethod -Method Post `
  -Uri http://localhost:8085/api/jobs `
  -ContentType 'application/json' `
  -Body '{"name":"flaky-job","payload":"fail until payload is changed","idempotencyKey":"flaky-demo","maxAttempts":2}'
```

4. Inspect job states.

```powershell
Start-Sleep -Seconds 5
Invoke-RestMethod http://localhost:8085/api/jobs
```

## Interview Close

Say: the interesting part is not the HTTP API. It is the durable job record, leadership lock, retry state, and execution audit trail.
