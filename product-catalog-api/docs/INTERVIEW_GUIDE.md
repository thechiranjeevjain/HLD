# Product Catalog API Interview Guide

## Two-Minute Pitch

This is a clean CRUD service for catalog data. It shows how a backend owns validation, persistence, duplicate business keys, database migrations, and consistent API error responses.

## What To Emphasize

- The SKU is a business identity and should be unique.
- Controllers translate HTTP into typed request models.
- Services hold business rules such as duplicate SKU handling.
- JPA repositories isolate database access.
- Flyway keeps schema creation repeatable.

## Request Flow

1. Client sends a product request.
2. `ProductController` validates JSON shape.
3. `ProductService` enforces SKU uniqueness and active/search rules.
4. `ProductRepository` persists to PostgreSQL.
5. `GlobalExceptionHandler` returns consistent errors for validation, duplicate SKU, and not-found cases.

## Tradeoffs

| Decision                 | Benefit                                  | Cost                                              |
| ------------------------ | ---------------------------------------- | ------------------------------------------------- |
| Single service           | Easy interview demo and simple ownership | Not split for search, inventory, or pricing scale |
| PostgreSQL               | Strong consistency for catalog records   | Requires migrations and DB lifecycle              |
| Service-layer validation | Testable business rules                  | More code than direct repository calls            |
| Docker Compose           | Reproducible local runtime               | Host Docker daemon must be healthy                |

## FAQ

Q: Why not use a document database?
A: The product shape is relational enough for a simple SQL table, and the project wants to demonstrate validation, constraints, and migrations.

Q: Where should duplicate SKU be enforced?
A: Both service logic and a database uniqueness constraint are useful. Service logic gives a friendly error; the database protects correctness.

Q: What would you add next?
A: pagination, optimistic locking, audit history, search indexing, image metadata, and admin authentication.
