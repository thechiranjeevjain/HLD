# Authentication Service

A Spring Boot authentication service with user registration, BCrypt password hashing, JWT bearer tokens, role-based access control, PostgreSQL persistence, and a local OAuth2-style identity endpoint for development workflows.

## Stack

- Java 17
- Spring Boot 3.3.6
- Spring Security
- JWT with signed HMAC tokens
- OAuth2 client dependency and local provider callback shape
- Spring Data JPA
- Flyway
- PostgreSQL
- Docker Compose

## Endpoints

| Method | Path | Purpose |
| --- | --- | --- |
| `POST` | `/api/auth/register` | Register a password user and return a JWT |
| `POST` | `/api/auth/login` | Login with email/password and return a JWT |
| `POST` | `/api/auth/oauth2/mock` | Provision or login an OAuth2 identity for local development |
| `GET` | `/api/users/me` | Return the authenticated user |
| `GET` | `/api/admin/users` | List users; requires `ADMIN` role |
| `GET` | `/actuator/health` | Health check |

## Run

```powershell
docker compose up --build
```

The API listens on `http://localhost:8081`.

To seed an admin user, set these environment variables before starting the API:

```powershell
$env:APP_ADMIN_EMAIL = "admin@example.com"
$env:APP_ADMIN_PASSWORD = "AdminPass123!"
```

## Smoke Test

Register:

```powershell
$auth = Invoke-RestMethod -Method Post `
  -Uri http://localhost:8081/api/auth/register `
  -ContentType 'application/json' `
  -Body '{
    "email": "user@example.com",
    "password": "UserPass123!",
    "displayName": "Capstone User"
  }'

$auth.accessToken
```

Call a protected endpoint:

```powershell
Invoke-RestMethod `
  -Uri http://localhost:8081/api/users/me `
  -Headers @{ Authorization = "Bearer $($auth.accessToken)" }
```

Use the local OAuth2-style identity flow:

```powershell
Invoke-RestMethod -Method Post `
  -Uri http://localhost:8081/api/auth/oauth2/mock `
  -ContentType 'application/json' `
  -Body '{
    "provider": "github",
    "providerSubject": "github-123",
    "email": "oauth@example.com",
    "displayName": "OAuth User"
  }'
```

## Verify

```powershell
mvn clean verify
```
