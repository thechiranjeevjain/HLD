# Notification Platform Demo Script

## Start

```powershell
cd G:\TechStudyNotes\SystemDesignProjects\notification-platform
docker compose up --build
```

API base URL: `http://localhost:8084`.

## Walkthrough

1. Create a successful email.

```powershell
$ok = Invoke-RestMethod -Method Post `
  -Uri http://localhost:8084/api/notifications `
  -ContentType 'application/json' `
  -Body '{"channel":"EMAIL","recipient":"user@example.com","subject":"Welcome","body":"Your account is ready"}'
```

2. Create a failing SMS.

```powershell
$fail = Invoke-RestMethod -Method Post `
  -Uri http://localhost:8084/api/notifications `
  -ContentType 'application/json' `
  -Body '{"channel":"SMS","recipient":"+1555000fail","body":"This simulated delivery will fail","maxAttempts":2}'
```

3. Wait, then inspect status and DLQ.

```powershell
Start-Sleep -Seconds 5
Invoke-RestMethod "http://localhost:8084/api/notifications/$($fail.id)"
Invoke-RestMethod http://localhost:8084/api/dead-letter
```

4. Discuss retries.

Say: this project shows at-least-once delivery pressure. Consumers and providers must tolerate duplicate attempts.
