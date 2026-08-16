# URL Shortener

A Spring Boot URL shortener with PostgreSQL persistence, Redis-backed rate limiting, redirect tracking, optional expiry, and Docker Compose for local infrastructure.

## Stack

- Java 17
- Spring Boot 3.3.6
- Spring Web
- Spring Data JPA
- Spring Data Redis
- Flyway
- PostgreSQL
- Redis
- Docker Compose

## Endpoints

| Method | Path                | Purpose                                  |
| ------ | ------------------- | ---------------------------------------- |
| `POST` | `/api/links`        | Create a short link                      |
| `GET`  | `/api/links/{code}` | Read short-link metadata and click count |
| `GET`  | `/{code}`           | Redirect to the original URL             |
| `GET`  | `/actuator/health`  | Health check                             |

## Run

```powershell
docker compose up --build
```

The API listens on `http://localhost:8082`.

## Smoke Test

Create a short link:

```powershell
$link = Invoke-RestMethod -Method Post `
  -Uri http://localhost:8082/api/links `
  -ContentType 'application/json' `
  -Body '{
    "originalUrl": "https://example.com/products/sku-1001",
    "ownerKey": "demo-user"
  }'

$link
```

Inspect metadata:

```powershell
Invoke-RestMethod "http://localhost:8082/api/links/$($link.code)"
```

Follow the redirect:

```powershell
Invoke-WebRequest "http://localhost:8082/$($link.code)" -MaximumRedirection 0
```

## Verify

```powershell
mvn clean verify
```
