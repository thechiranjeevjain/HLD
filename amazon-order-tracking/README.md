# Amazon Order Tracking — Event-First System Design

A runnable interview portfolio project for tracking millions of orders per day across unreliable carrier feeds. It demonstrates the hardest parts of the design instead of hiding them in slides: duplicate delivery, out-of-order scans, immutable history, derived state, multi-package reads, cache invalidation, authorization, and support auditing.

## What works

- `GET /orders/{orderId}/tracking`: aggregate status plus a unified and per-package timeline.
- `GET /orders/{orderId}/shipments`: package, carrier, tracking number, current state.
- `POST /carrier/events`: validates, hashes, deduplicates, appends, and derives state.
- Event-time replay prevents a late `PACKED` scan from rolling a `SHIPPED` package backward.
- H2 models the durable event/read stores locally; repository seams map to DynamoDB/Cassandra at scale.
- Caffeine provides an executable 10-second cache/request-coalescing model; Redis is the production substitution.
- Customer ownership and support-role checks; every successful read writes an access audit row.
- Responsive browser dashboard, health/metrics endpoints, demo data, and integration tests.

## Run it

Requirements: Java 17+ and Maven 3.9+.

```powershell
cd G:\TechStudyNotes\SystemDesignProjects\amazon-order-tracking
mvn clean verify
mvn spring-boot:run
```

Open [http://127.0.0.1:8080](http://127.0.0.1:8080). The seeded buyer is `user-123`, and the order is `ORD-1001`.

Generate a live scan in another terminal:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\demo-events.ps1
```

Or call the API directly:

```powershell
Invoke-RestMethod http://127.0.0.1:8080/orders/ORD-1001/tracking `
  -Headers @{"X-Actor-Id"="user-123"}
```

The identity headers are deliberately transparent demo authentication. Production uses verified JWT/OIDC claims at the gateway—never user-supplied identity headers.

## Project map

```text
src/main/java/.../api       HTTP contract, validation, error mapping
src/main/java/.../domain    immutable events and derived shipment state
src/main/java/.../service   authorization, dedupe, replay, cache, audit
src/main/resources/static   live customer dashboard
src/test                    end-to-end API and failure-behavior tests
docs/                       architecture, capacity, interview, operations
scripts/                    readable live-demo event generator
```

## Documentation

- [Architecture and diagrams](docs/ARCHITECTURE.md)
- [Interview guide](docs/INTERVIEW_GUIDE.md)
- [Failure drills and operations](docs/OPERATIONS.md)
- [Live demo script](docs/DEMO_SCRIPT.md)

## Local versus production

| Concern | Runnable local slice | Production mapping |
| --- | --- | --- |
| Ingest | Spring REST transaction | API Gateway → Kafka/Kinesis |
| Event store | H2 `tracking_event` | DynamoDB/Cassandra or Kafka + object archive |
| Projection | deterministic synchronous replay | partitioned stream processor + checkpoints/watermarks |
| Read model | indexed H2 tables | DynamoDB/Cassandra keyed by order/shipment |
| Hot cache | Caffeine, 10 s TTL | Redis cluster + distributed single-flight |
| Auth | explicit demo headers | OIDC/JWT and policy enforcement point |

This repo proves the domain behavior locally. It does not claim a running Kafka, Redis, DynamoDB, multi-region, or carrier integration.
