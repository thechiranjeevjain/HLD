# System Design Projects

This folder is organized for hands-on backend learning and interview explanation. Projects live directly under this directory. Old wrapper folders such as `capstone-projects` and `Backend5` are archived locally under `_archive`; generated learning metadata lives locally under `_meta`. Both local-only folders are ignored by Git.

## Recommended Learning Path

Do not merge everything into one giant application. For interviews, it is stronger to have one or two deep flagship projects plus smaller focused labs you can explain quickly.

1. `mini-nasdaq-risk-platform`
   - Best live interview demo for risk/exchange discussion.
   - Includes a standalone `pretrade-risk-engine` with accepted/rejected orders, FIX input, fail-closed checks, market-data freshness, kill switch, circuit breaker, audit trail, race-condition demo, and P&L.
2. `exchange-lite`
   - Best low-level exchange internals project.
   - Shows order book, binary protocol, engine runtime, IPC, sidecar, and CLI operations.
3. `mini-risk-management-platform`
   - Best broader microservices/platform project.
   - Shows API gateway, order/risk/history/notification services, PostgreSQL, Kafka, Redis, Docker Compose, Kubernetes, metrics, and dashboards.
4. Focused services and labs
   - Use these to practice one concept at a time: auth, CRUD, URL shortener, notification retries, scheduling, ride matching, fraud scoring, caching, queues, web servers, and KV storage.

## Active Projects

| Folder | Purpose |
| --- | --- |
| `product-catalog-api` | CRUD API with product lifecycle, validation, duplicate SKU handling, PostgreSQL/Flyway shape. |
| `authentication-service` | Registration, login, JWT, roles, mock OAuth2 flow. |
| `url-shortener` | Short-code generation, redirects, hit tracking, Redis-style rate limiting. |
| `distributed-database` | TCP key-value cluster with replication, quorum behavior, consistent hashing, recovery. |
| `distributed-database/labs/replicated-log-simulation` | Focused replicated log and partition/slowness simulation. |
| `ecommerce-backend` | Inventory reservation, order creation, payments, order events. |
| `notification-platform` | Multi-channel notification, retry, dead-letter behavior. |
| `distributed-task-scheduler` | Job scheduling, retries, worker flow, leader/lock concept. |
| `ride-sharing-backend` | Driver location, nearby search, ride request/status flow. |
| `fraud-detection-platform` | Transaction event ingestion and risk scoring. |
| `hotel-booking-service` | Hotel read/search/delete API, basic auth, browser UI, OpenAPI, metrics. |
| `employee-document-upload-system` | Signed upload intent, document review policy, security roles, infra docs. |
| `exchange-lite` | Matching engine, risk checks, binary protocol, engine/sidecar/CLI runtime. |
| `mini-nasdaq-risk-platform` | Nasdaq-style risk platform plus live pre-trade risk engine demo. |
| `mini-risk-management-platform` | Java 21 microservices risk platform with Docker/Kubernetes/observability. |
| `cache-lab` | LRU plus TTL cache behavior and metrics. |
| `message-queue-lab` | Queue retry, acknowledgement, and dead-letter behavior. |
| `mini-kv-storage-engine` | WAL-backed key-value store with TTL and compaction. |
| `web-server-lab` | Minimal blocking HTTP server. |

## Verification Notes

Last local audit: 2026-08-05.

- Java 17 and Maven 3.9 are available as the default toolchain.
- JDK 21 is not installed locally. `mini-risk-management-platform` targets Java 21; it was verified with the installed IntelliJ JDK 25 using `-Dnet.bytebuddy.experimental=true`.
- Docker and kubectl are installed with Docker Desktop but are not on PATH in this terminal. Docker Compose files and Kubernetes kustomizations render, but full container and cluster smoke tests require a responsive Docker engine and Kubernetes context.
- The old `Booking` folder has been archived as `_archive/2026-08-05-flattened-wrappers/Booking-legacy`. Use `hotel-booking-service` as the active copy.

## Common Verification Commands

For most Java 17 projects:

```powershell
mvn test
mvn "-DskipTests" package
```

For `employee-document-upload-system`:

```powershell
cd app
mvn test
mvn "-DskipTests" package
```

For `mini-risk-management-platform` until a real JDK 21 is installed:

```powershell
$jdk = "C:\Users\Chiranjeev Jain\.jdks\openjdk-25"
$env:JAVA_HOME = $jdk
$env:Path = "$jdk\bin;$env:Path"
mvn "-Dnet.bytebuddy.experimental=true" test
mvn "-Dnet.bytebuddy.experimental=true" "-DskipTests" package
```

For the fast live pre-trade demo:

```powershell
cd G:\TechStudyNotes\SystemDesignProjects\mini-nasdaq-risk-platform
mvn -pl pretrade-risk-engine spring-boot:run
```

Open `http://localhost:8090`.

For the exchange engine demo:

```powershell
cd G:\TechStudyNotes\SystemDesignProjects\exchange-lite
mvn test
mvn package
```

Then follow the three-terminal engine, sidecar, and CLI commands in `exchange-lite/README.md`.
