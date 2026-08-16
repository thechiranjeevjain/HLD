# Deployment Guide

## Local Deployment

Use Docker Compose when you want the full stack on one machine.

```powershell
docker compose up --build
```

Stop and keep volumes:

```powershell
docker compose down
```

Stop and delete volumes:

```powershell
docker compose down -v
```

Deleting volumes removes PostgreSQL, Redis, Kafka, Prometheus, and Grafana data.

## Kubernetes Deployment

Build images:

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
kubectl -n mini-risk rollout status deployment/api-gateway
kubectl -n mini-risk get pods,svc,hpa,pvc,ingress
```

Rollback a deployment:

```powershell
kubectl -n mini-risk rollout undo deployment/order-service
```

Delete:

```powershell
kubectl delete -k k8s
```

PersistentVolumes use `Retain`, so data may remain after deleting the app. That is intentional for labs about persistence and cleanup.

## Environment Variables

| Variable                         | Owner                     | Meaning                             |
| -------------------------------- | ------------------------- | ----------------------------------- |
| `SPRING_DATASOURCE_URL`          | stateful services         | JDBC URL for each service database. |
| `SPRING_DATASOURCE_USERNAME`     | stateful services         | PostgreSQL username from Secret.    |
| `SPRING_DATASOURCE_PASSWORD`     | stateful services         | PostgreSQL password from Secret.    |
| `SPRING_KAFKA_BOOTSTRAP_SERVERS` | Kafka clients             | Kafka broker address.               |
| `SPRING_DATA_REDIS_HOST`         | risk-service              | Redis host for cache.               |
| `ORDER_SERVICE_URL`              | api-gateway               | Downstream order service URL.       |
| `RISK_SERVICE_URL`               | order-service             | Downstream risk service URL.        |
| `HISTORY_SERVICE_URL`            | api-gateway, risk-service | Downstream history service URL.     |
| `ORDER_EVENTS_TOPIC`             | Kafka clients             | Topic name for order events.        |

## Smoke Test

```powershell
$body = @{
  clientId = "CLIENT-A"
  symbol = "AAPL"
  side = "BUY"
  quantity = 100
  price = 150.25
} | ConvertTo-Json

Invoke-RestMethod -Method Post -Uri "http://localhost:8080/api/orders" -ContentType "application/json" -Body $body
Start-Sleep -Seconds 2
Invoke-RestMethod "http://localhost:8080/api/exposures/CLIENT-A/AAPL"
```

Expected result: order is `ACCEPTED`; exposure eventually shows net quantity `100`.
