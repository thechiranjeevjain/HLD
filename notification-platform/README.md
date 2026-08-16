# Notification Platform

A Spring Boot notification platform with email, SMS, and push channels; scheduled delivery; retry backoff; and dead-letter queue records.

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

| Method | Path                            | Purpose                           |
| ------ | ------------------------------- | --------------------------------- |
| `POST` | `/api/notifications`            | Create and enqueue a notification |
| `GET`  | `/api/notifications/{id}`       | Read notification status          |
| `POST` | `/api/notifications/{id}/retry` | Manually requeue a notification   |
| `GET`  | `/api/dead-letter`              | List dead-letter queue records    |
| `GET`  | `/actuator/health`              | Health check                      |

## Run

```powershell
docker compose up --build
```

The API listens on `http://localhost:8084`.

## Smoke Test

Send a successful email:

```powershell
Invoke-RestMethod -Method Post `
  -Uri http://localhost:8084/api/notifications `
  -ContentType 'application/json' `
  -Body '{
    "channel": "EMAIL",
    "recipient": "user@example.com",
    "subject": "Welcome",
    "body": "Your account is ready"
  }'
```

Send a notification that will retry and move to DLQ:

```powershell
Invoke-RestMethod -Method Post `
  -Uri http://localhost:8084/api/notifications `
  -ContentType 'application/json' `
  -Body '{
    "channel": "SMS",
    "recipient": "+1555000fail",
    "body": "This simulated delivery will fail",
    "maxAttempts": 2
  }'
```

Inspect DLQ:

```powershell
Invoke-RestMethod http://localhost:8084/api/dead-letter
```

## Verify

```powershell
mvn clean verify
```
