# Hotel Booking Service

Spring Boot hotel API project at `G:\TechStudyNotes\SystemDesignProjects\hotel-booking-service`.

## What Is Included

- REST API for `GET /hotel/{id}`, `DELETE /hotel/{id}`, and `GET /search/{cityId}`.
- OpenAPI contract at `src/main/resources/static/openapi.yaml`.
- Delegate-style API layer between controller and service.
- MVP browser UI at `http://localhost:8080/`.
- Spring Security `SecurityFilterChain` with Basic auth, form login, and role checks.
- Redis-ready cache for city search results.
- Kafka hotel delete events.
- Dockerfile and Docker Compose for app, Redis, Kafka, Prometheus, and OpenTelemetry Collector.
- EKS-ready Kubernetes manifests under `k8s/eks`.
- Actuator, Prometheus metrics, trace IDs in logs, and OTLP trace export config.

## Local Run

```powershell
mvn spring-boot:run
```

Default users:

- `user` / `password`: read and search
- `admin` / `admin`: read, search, and delete

## UI And API

- UI: `http://localhost:8080/`
- OpenAPI YAML: `http://localhost:8080/openapi.yaml`
- Metrics: `http://localhost:8080/actuator/prometheus`
- Health: `http://localhost:8080/actuator/health`

Example API calls:

```powershell
$user = @{ Authorization = "Basic " + [Convert]::ToBase64String([Text.Encoding]::ASCII.GetBytes("user:password")) }
$admin = @{ Authorization = "Basic " + [Convert]::ToBase64String([Text.Encoding]::ASCII.GetBytes("admin:admin")) }

Invoke-RestMethod http://localhost:8080/hotel/1 -Headers $user
Invoke-RestMethod http://localhost:8080/search/1 -Headers $user
Invoke-RestMethod -Method Delete http://localhost:8080/hotel/4 -Headers $admin
```

## Build And Test

```powershell
mvn test
mvn package
```

If your global Maven cache is locked, use the project-local cache:

```powershell
mvn "-Dmaven.repo.local=G:\TechStudyNotes\SystemDesignProjects\hotel-booking-service\.m2repo" test
mvn "-Dmaven.repo.local=G:\TechStudyNotes\SystemDesignProjects\hotel-booking-service\.m2repo" package
```

## Docker Compose

```powershell
docker compose up --build
```

Compose enables Redis caching and Kafka events:

- App: `http://localhost:8080/`
- Prometheus: `http://localhost:9090/`
- Redis: `localhost:6379`
- Kafka: `localhost:9092`

## EKS

Update the image in `k8s/eks/deployment.yaml`, replace secrets in `k8s/eks/secret.example.yaml`, then apply:

```powershell
kubectl apply -k k8s/eks
```

The included Redis and Kafka manifests are single-node development defaults. For production EKS, use managed or operator-backed Redis/Kafka.

## Data

The local project uses an in-memory H2 database loaded from `src/main/resources/data.sql`.
