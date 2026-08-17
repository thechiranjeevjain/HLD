# E-Commerce Backend

A Spring Boot e-commerce backend with inventory management, order placement, payment simulation, stock reservation, and Kafka order-event publishing.

## Stack

- Java 17
- Spring Boot 3.3.6
- Spring Web
- Spring Data JPA
- Flyway
- PostgreSQL
- Spring Kafka
- Docker Compose

## Endpoints

| Method | Path | Purpose |
| --- | --- | --- |
| `POST` | `/api/inventory` | Create or replace inventory for a SKU |
| `GET` | `/api/inventory` | List inventory |
| `POST` | `/api/orders` | Place an order and reserve stock |
| `GET` | `/api/orders/{id}` | Read an order |
| `POST` | `/api/orders/{id}/payments` | Capture or decline a simulated payment |
| `GET` | `/actuator/health` | Health check |

## Run

```powershell
docker compose up --build
```

The API listens on `http://localhost:8083`.

## Smoke Test

Create inventory:

```powershell
Invoke-RestMethod -Method Post `
  -Uri http://localhost:8083/api/inventory `
  -ContentType 'application/json' `
  -Body '{
    "sku": "SKU-1001",
    "name": "Mechanical Keyboard",
    "price": 129.99,
    "currency": "USD",
    "stockQuantity": 10
  }'
```

Place an order:

```powershell
$order = Invoke-RestMethod -Method Post `
  -Uri http://localhost:8083/api/orders `
  -ContentType 'application/json' `
  -Body '{
    "customerId": "customer-1",
    "items": [
      { "sku": "SKU-1001", "quantity": 2 }
    ]
  }'

$order
```

Capture payment:

```powershell
Invoke-RestMethod -Method Post `
  -Uri "http://localhost:8083/api/orders/$($order.id)/payments" `
  -ContentType 'application/json' `
  -Body '{ "paymentToken": "tok_success_demo" }'
```

Decline a payment by using a token that starts with `fail`.

## Kafka Events

The service publishes compact JSON messages to the `order-events` topic for order creation and payment outcomes.

## Verify

```powershell
mvn clean verify
```
