# Product Catalog API

A Spring Boot product catalog API with PostgreSQL persistence, Flyway migrations, validation, centralized error responses, and Docker Compose for local runtime.

## Stack

- Java 17
- Spring Boot 3.3.6
- Spring Web
- Spring Data JPA
- Bean Validation
- Flyway
- PostgreSQL
- Docker Compose

## Endpoints

| Method   | Path                 | Purpose                                                   |
| -------- | -------------------- | --------------------------------------------------------- |
| `GET`    | `/api/products`      | List products with optional `search` and `active` filters |
| `GET`    | `/api/products/{id}` | Get one product                                           |
| `POST`   | `/api/products`      | Create a product                                          |
| `PUT`    | `/api/products/{id}` | Replace a product                                         |
| `DELETE` | `/api/products/{id}` | Delete a product                                          |
| `GET`    | `/actuator/health`   | Health check                                              |

## Run With Docker Compose

```powershell
docker compose up --build
```

The API listens on `http://localhost:8080` and PostgreSQL listens on `localhost:5432`.

## Run With Local Maven

Start PostgreSQL first:

```powershell
docker compose up -d postgres
```

Then run the API:

```powershell
mvn spring-boot:run
```

## Smoke Test

Create a product:

```powershell
$created = Invoke-RestMethod -Method Post `
  -Uri http://localhost:8080/api/products `
  -ContentType 'application/json' `
  -Body '{
    "sku": "SKU-1001",
    "name": "Mechanical Keyboard",
    "description": "Hot-swappable keyboard with compact layout",
    "price": 129.99,
    "currency": "USD",
    "stockQuantity": 25,
    "active": true
  }'

$created
```

List products:

```powershell
Invoke-RestMethod http://localhost:8080/api/products
```

Search products:

```powershell
Invoke-RestMethod "http://localhost:8080/api/products?search=keyboard&active=true"
```

Update a product:

```powershell
Invoke-RestMethod -Method Put `
  -Uri "http://localhost:8080/api/products/$($created.id)" `
  -ContentType 'application/json' `
  -Body '{
    "sku": "SKU-1001",
    "name": "Mechanical Keyboard Pro",
    "description": "Updated model",
    "price": 149.99,
    "currency": "USD",
    "stockQuantity": 15,
    "active": true
  }'
```

Delete a product:

```powershell
Invoke-RestMethod -Method Delete "http://localhost:8080/api/products/$($created.id)"
```

## Verify

```powershell
mvn clean verify
```
