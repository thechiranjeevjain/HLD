# Hotel Booking Service Interview Guide

## Two-Minute Pitch

This service exposes a hotel lookup and city search API with a small browser UI, OpenAPI contract, role-protected deletion, cache-ready city search, Kafka delete events, metrics, traces, Docker Compose, and Kubernetes manifests. It is a good demo for taking a basic CRUD/search service toward operability.

## What To Emphasize

- The API contract is explicit through OpenAPI.
- Read/search operations are available to normal users.
- Delete is restricted to admins.
- Redis caching is useful for repeated city searches.
- Kafka delete events let downstream systems react.
- Actuator and Prometheus make the service observable.

## Request Flow

1. Browser or client calls hotel or search endpoints.
2. Spring Security authenticates with Basic auth or form login.
3. Controller/delegate layer calls `HotelService`.
4. Service reads H2-backed local data and maps to DTOs.
5. Delete events are published through the configured event publisher.

## Tradeoffs

| Decision | Benefit | Cost |
| --- | --- | --- |
| H2 local database | Simple local demo | Not production persistence |
| Basic auth defaults | Easy local access control demo | Real deployments need external identity |
| Redis-ready caching | Shows read optimization | Cache invalidation must be handled |
| Kafka delete event | Decouples side effects | Needs broker operations |
| EKS manifests | Interview-ready deployment story | Runtime requires a real cluster context |

## FAQ

Q: Why keep OpenAPI in the repo?
A: It gives a stable API contract that clients and reviewers can inspect independent of implementation.

Q: Why emit delete events?
A: Search indexes, audit processors, or notification systems may need to react when a hotel is removed.

Q: What would you add next?
A: real PostgreSQL persistence, pagination, cache invalidation tests, audit logs, OAuth2, and reservation workflows.
