# Fraud Detection Platform

A Spring Boot fraud detection platform with synchronous transaction scoring, Kafka event ingestion, Redis velocity checks, PostgreSQL audit storage, and a rule-engine based risk score.

## Stack

- Java 17
- Spring Boot 3.3.6
- Spring Web
- Spring Data JPA
- Spring Data Redis
- Spring Kafka
- Flyway
- PostgreSQL
- Redis
- Docker Compose

## Endpoints

| Method | Path                         | Purpose                        |
| ------ | ---------------------------- | ------------------------------ |
| `POST` | `/api/events/transactions`   | Ingest and score a transaction |
| `GET`  | `/api/risks/{transactionId}` | Read a fraud decision          |
| `GET`  | `/actuator/health`           | Health check                   |

Kafka listener consumes the same transaction schema from the `transaction-events` topic and is idempotent by `transactionId`.

## Run

```powershell
docker compose up --build
```

The API listens on `http://localhost:8087`.

## Smoke Test

Score a low-risk transaction:

```powershell
Invoke-RestMethod -Method Post `
  -Uri http://localhost:8087/api/events/transactions `
  -ContentType 'application/json' `
  -Body '{
    "transactionId": "txn-1001",
    "userId": "user-1",
    "amount": 125.50,
    "currency": "USD",
    "merchantCategory": "GROCERY",
    "country": "US",
    "homeCountry": "US",
    "cardPresent": true
  }'
```

Score a high-risk transaction:

```powershell
Invoke-RestMethod -Method Post `
  -Uri http://localhost:8087/api/events/transactions `
  -ContentType 'application/json' `
  -Body '{
    "transactionId": "txn-9001",
    "userId": "user-2",
    "amount": 6200.00,
    "currency": "USD",
    "merchantCategory": "CRYPTO",
    "country": "SG",
    "homeCountry": "US",
    "cardPresent": false
  }'
```

Read the decision:

```powershell
Invoke-RestMethod http://localhost:8087/api/risks/txn-9001
```

## Verify

```powershell
mvn clean verify
```
