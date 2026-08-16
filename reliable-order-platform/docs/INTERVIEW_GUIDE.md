# Reliable Order Platform Interview Guide

## Two-Minute Pitch

This project teaches reliable backend order processing. It uses one Java 21 Spring Boot deployable, PostgreSQL for durable state, Redis as a cache-aside optimization, Kafka for asynchronous fulfillment, and a transactional outbox to avoid the database/event dual-write bug. The key claim is at-least-once delivery with idempotent effects.

## What To Emphasize

- `Idempotency-Key` prevents duplicate order creation on client retry.
- Order, audit, and outbox rows commit in one database transaction.
- The outbox poller publishes after commit and marks events published.
- The consumer records processed event IDs so duplicate Kafka delivery is safe.
- Redis failure degrades to database reads; PostgreSQL remains authoritative.
- JWT roles separate customer and support access.
- Kubernetes, Terraform, Prometheus, and Grafana are present as interview-shaped ops material.

## Boundary Against E-Commerce Backend

`ecommerce-backend` is the product flow project: inventory, order placement, and payment simulation. `reliable-order-platform` is the reliability project: idempotency, outbox, dedupe, auth, cache failure policy, and operational deployment.

## Tradeoffs

| Decision                  | Benefit                                          | Cost                                 |
| ------------------------- | ------------------------------------------------ | ------------------------------------ |
| Modular monolith          | One runnable deployable with clear package seams | Cannot scale modules independently   |
| Transactional outbox      | Closes DB/event dual-write gap                   | Requires poller and cleanup policy   |
| At-least-once plus dedupe | Honest distributed-systems semantics             | Consumers must be idempotent         |
| Cache-aside Redis         | Faster repeated reads                            | Stale reads until eviction/TTL       |
| JWT/OIDC locally          | Realistic auth story                             | Requires local Keycloak in full demo |

## FAQ

Q: Is this exactly-once processing?
A: No. The honest model is at-least-once delivery with idempotent effects.

Q: Why not publish directly to Kafka inside the request?
A: A database commit can succeed while Kafka publish fails, or the reverse. The outbox gives a durable retry boundary.

Q: What would you add next?
A: migration jobs, dead-letter handling, order cancellation, inventory reservation, payment integration, distributed tracing, and cloud secret rotation.
