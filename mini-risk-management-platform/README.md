# Mini Risk Management Platform

A production engineering interview lab inspired by a simplified pre-trade risk platform.

The goal is not to maximize features. The goal is to make each senior backend concept concrete: Java processes, Spring request handling, service calls, PostgreSQL persistence, Kafka event flow, Redis caching, Docker isolation, Kubernetes scheduling, probes, volumes, observability, and production debugging.

## What You Build And Study

- `api-gateway`: public entry point used by Docker Compose and Kubernetes Ingress.
- `order-service`: accepts orders, validates input, calls `risk-service`, stores orders, publishes Kafka events.
- `risk-service`: evaluates order quantity, position, and daily exposure limits. Uses PostgreSQL for limits and Redis as a read-through cache.
- `history-service`: consumes accepted order events, stores exposure history, exposes running totals.
- `notification-service`: consumes Kafka order events and prints simulated email notifications.
- `PostgreSQL`: durable state for orders, limits, and exposure events.
- `Kafka`: event backbone between order, history, and notification.
- `Redis`: low-latency cache for risk limits.
- `Prometheus` and `Grafana`: metrics and dashboards from Spring Boot Actuator.

## Requirements

- Java 21
- Maven 3.9+
- Docker and Docker Compose
- Optional: Kubernetes cluster, `kubectl`, and an NGINX Ingress controller

This repository targets Java 21. The source intentionally avoids obscure language features so the architecture remains the focus.

## Quick Start With Docker Compose

From this folder:

```powershell
docker compose up --build
```

Submit an order through the gateway:

```powershell
$body = @{
  clientId = "CLIENT-A"
  symbol = "AAPL"
  side = "BUY"
  quantity = 100
  price = 150.25
} | ConvertTo-Json

Invoke-RestMethod -Method Post -Uri "http://localhost:8080/api/orders" -ContentType "application/json" -Body $body
```

Query exposure:

```powershell
Invoke-RestMethod "http://localhost:8080/api/exposures/CLIENT-A/AAPL"
```

Useful local URLs:

- API Gateway: `http://localhost:8080`
- Order service actuator: `http://localhost:8081/actuator/health`
- Risk service actuator: `http://localhost:8082/actuator/health`
- History service actuator: `http://localhost:8083/actuator/health`
- Notification service actuator: `http://localhost:8084/actuator/health`
- Prometheus: `http://localhost:9090`
- Grafana: `http://localhost:3000` with `admin/admin`

## Kubernetes Quick Start

Build local images:

```powershell
docker build -f api-gateway/Dockerfile -t mini-risk/api-gateway:0.1.0 .
docker build -f order-service/Dockerfile -t mini-risk/order-service:0.1.0 .
docker build -f risk-service/Dockerfile -t mini-risk/risk-service:0.1.0 .
docker build -f history-service/Dockerfile -t mini-risk/history-service:0.1.0 .
docker build -f notification-service/Dockerfile -t mini-risk/notification-service:0.1.0 .
```

Deploy:

```powershell
kubectl apply -k k8s
kubectl -n mini-risk get pods,svc,hpa,ingress,pv,pvc
```

For local Ingress, add this host entry:

```text
127.0.0.1 mini-risk.local
```

Then call:

```powershell
Invoke-RestMethod "http://mini-risk.local/actuator/health"
```

## Run Tests

Fast unit tests:

```powershell
mvn test
```

Opt-in Testcontainers migration tests:

```powershell
$env:RUN_TESTCONTAINERS = "true"
mvn test
```

## Study Path

Follow this order:

1. Java process and Spring Boot request lifecycle
2. Order request flow
3. PostgreSQL schema and Flyway migrations
4. Kafka event flow
5. Redis cache behavior
6. Docker image and container internals
7. Docker Compose networking and volumes
8. Kubernetes Pods, Deployments, Services, and probes
9. Ingress, DNS, and service discovery
10. Observability with Actuator, Prometheus, and Grafana
11. Production incident drills
12. System design interview review

## Documentation Map

- [Architecture](docs/ARCHITECTURE.md)
- [Diagrams](docs/DIAGRAMS.md)
- [Project Structure](docs/PROJECT_STRUCTURE.md)
- [Deployment Guide](docs/DEPLOYMENT_GUIDE.md)
- [Docker Guide](docs/DOCKER_GUIDE.md)
- [Kubernetes Guide](docs/KUBERNETES_GUIDE.md)
- [Demo Script](docs/DEMO_SCRIPT.md)
- [Technology Deep Dives](docs/TECHNOLOGY_DEEP_DIVES.md)
- [Linux Internals](docs/LINUX_INTERNALS.md)
- [Debugging Guide](docs/DEBUGGING_GUIDE.md)
- [Production Incidents](docs/PRODUCTION_INCIDENTS.md)
- [Hands-On Labs](docs/HANDS_ON_LABS.md)
- [Production Guide](docs/PRODUCTION_GUIDE.md)
- [Interview Guide](docs/INTERVIEW_GUIDE.md)
- [FAQ](docs/FAQ.md)
- [Learning Guide](docs/LEARNING_GUIDE.md)
