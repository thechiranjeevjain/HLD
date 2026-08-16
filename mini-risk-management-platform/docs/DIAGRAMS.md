# Mermaid Diagrams

## High-Level Design

### Overall Architecture

```mermaid
flowchart LR
  Client["Client"] --> Gateway["api-gateway"]
  Gateway --> Order["order-service"]
  Order --> Risk["risk-service"]
  Risk --> HistoryApi["history-service API"]
  Risk --> Redis["Redis risk-limit cache"]
  Risk --> RiskDb[("PostgreSQL risk DB")]
  Order --> OrderDb[("PostgreSQL orders DB")]
  Order --> Kafka["Kafka order-events topic"]
  Kafka --> HistoryConsumer["history-service consumer"]
  Kafka --> Notification["notification-service"]
  HistoryConsumer --> HistoryDb[("PostgreSQL history DB")]
  Prometheus["Prometheus"] --> Gateway
  Prometheus --> Order
  Prometheus --> Risk
  Prometheus --> HistoryApi
  Prometheus --> Notification
  Grafana["Grafana"] --> Prometheus
```

## Low-Level Design

### Request Flow

```mermaid
sequenceDiagram
  participant C as Client
  participant G as api-gateway
  participant O as order-service
  participant R as risk-service
  participant H as history-service
  participant DB as PostgreSQL
  participant K as Kafka

  C->>G: POST /api/orders
  G->>O: POST /orders
  O->>R: POST /risk/check
  R->>DB: Read risk_limits
  R->>H: GET /exposures/{clientId}/{symbol}
  H->>DB: Aggregate exposure_events
  H-->>R: ExposureSummary
  R-->>O: ACCEPT or REJECT
  O->>DB: Insert order
  O->>K: Publish OrderEvent
  O-->>G: OrderResponse
  G-->>C: 202 Accepted
```

### Kafka Flow

```mermaid
flowchart LR
  Order["order-service producer"] --> Topic["Kafka topic: order-events"]
  Topic --> History["history-service group"]
  Topic --> Notify["notification-service group"]
  History --> HistoryDb[("exposure_events")]
  Notify --> Logs["Simulated email log"]
```

### Redis Flow

```mermaid
sequenceDiagram
  participant R as risk-service
  participant Redis as Redis
  participant DB as risk PostgreSQL

  R->>Redis: GET risk-limit:CLIENT-A:AAPL
  alt cache hit
    Redis-->>R: RiskLimitSnapshot
  else cache miss or Redis failure
    R->>DB: SELECT risk_limits
    DB-->>R: limit row
    R->>Redis: SET key with TTL
  end
```

### Database Flow

```mermaid
flowchart TB
  subgraph "orders database"
    Orders["orders table"]
  end
  subgraph "risk database"
    Limits["risk_limits table"]
  end
  subgraph "history database"
    Exposure["exposure_events table"]
  end
  OrderSvc["order-service"] --> Orders
  RiskSvc["risk-service"] --> Limits
  HistorySvc["history-service"] --> Exposure
```

### Docker Architecture

```mermaid
flowchart TB
  Host["Host OS"] --> DockerDaemon["Docker daemon"]
  DockerDaemon --> Bridge["risk-net bridge network"]
  Bridge --> Gateway["api-gateway container"]
  Bridge --> Order["order-service container"]
  Bridge --> Risk["risk-service container"]
  Bridge --> History["history-service container"]
  Bridge --> Kafka["kafka container"]
  Bridge --> Redis["redis container"]
  Bridge --> Postgres["postgres container"]
  Postgres --> PgVolume["postgres-data volume"]
  Kafka --> KafkaVolume["kafka-data volume"]
  Redis --> RedisVolume["redis-data volume"]
```

### Deployment Flow

```mermaid
flowchart LR
  Dev["Developer"] --> Maven["mvn test"]
  Maven --> DockerBuild["docker build service images"]
  DockerBuild --> Compose["docker compose up"]
  DockerBuild --> KubeApply["kubectl apply -k k8s"]
  KubeApply --> Deployments["Kubernetes Deployments"]
  Deployments --> Pods["Pods"]
  Pods --> Services["Services"]
  Services --> Ingress["Ingress"]
```

### Kubernetes Architecture

```mermaid
flowchart TB
  Ingress["Ingress mini-risk.local"] --> GatewaySvc["Service api-gateway"]
  GatewaySvc --> GatewayPods["api-gateway Pods"]
  GatewayPods --> OrderSvc["Service order-service"]
  OrderSvc --> OrderPods["order-service Pods"]
  OrderPods --> RiskSvc["Service risk-service"]
  RiskSvc --> RiskPods["risk-service Pods"]
  RiskPods --> HistorySvc["Service history-service"]
  HistorySvc --> HistoryPods["history-service Pods"]
  RiskPods --> RedisSvc["Service redis"]
  OrderPods --> KafkaSvc["Service kafka"]
  HistoryPods --> KafkaSvc
  RedisSvc --> RedisPod["redis Pod"]
  KafkaSvc --> KafkaPod["kafka Pod"]
  OrderPods --> PostgresSvc["Service postgres"]
  RiskPods --> PostgresSvc
  HistoryPods --> PostgresSvc
  PostgresSvc --> PostgresPod["postgres Pod"]
```

### Networking

```mermaid
flowchart LR
  Browser["Browser or curl"] --> HostPort["localhost:8080"]
  HostPort --> GatewayContainer["api-gateway:8080"]
  GatewayContainer --> Dns["Docker DNS"]
  Dns --> OrderName["order-service"]
  OrderName --> OrderContainer["order-service:8081"]
  OrderContainer --> RiskName["risk-service"]
  RiskName --> RiskContainer["risk-service:8082"]
```

### Kubernetes Service Discovery

```mermaid
sequenceDiagram
  participant Pod as order-service Pod
  participant DNS as CoreDNS
  participant SVC as risk-service ClusterIP
  participant RiskPod as risk-service Pod

  Pod->>DNS: Resolve risk-service.mini-risk.svc.cluster.local
  DNS-->>Pod: ClusterIP
  Pod->>SVC: TCP connect port 8082
  SVC->>RiskPod: kube-proxy routes to endpoint
```

### Ingress Flow

```mermaid
flowchart LR
  User["User"] --> DNS["mini-risk.local"]
  DNS --> IngressController["NGINX Ingress Controller"]
  IngressController --> IngressRule["Ingress rule path /"]
  IngressRule --> GatewayService["api-gateway Service"]
  GatewayService --> GatewayPod["api-gateway Pod"]
```

### Storage

```mermaid
flowchart TB
  App["Stateful container"] --> Mount["Volume mount path"]
  Mount --> PVC["PersistentVolumeClaim"]
  PVC --> PV["PersistentVolume"]
  PV --> HostPath["Host path or cloud disk"]
```

### Volume Mounts

```mermaid
flowchart LR
  PostgresPod["postgres Pod"] --> PgMount["/var/lib/postgresql/data"]
  PgMount --> PgPvc["postgres-pvc"]
  KafkaPod["kafka Pod"] --> KafkaMount["/tmp/kafka-logs"]
  KafkaMount --> KafkaPvc["kafka-pvc"]
  RedisPod["redis Pod"] --> RedisMount["/data"]
  RedisMount --> RedisPvc["redis-pvc"]
```

### Pod Lifecycle

```mermaid
stateDiagram-v2
  [*] --> Pending
  Pending --> ContainerCreating
  ContainerCreating --> Running
  Running --> Ready: readiness probe succeeds
  Ready --> NotReady: readiness probe fails
  Running --> Restarting: liveness probe fails
  Restarting --> Running
  Running --> Terminating: delete or rollout
  Terminating --> [*]
```

### Container Lifecycle

```mermaid
stateDiagram-v2
  [*] --> ImagePull
  ImagePull --> Create
  Create --> Start
  Start --> Running
  Running --> Exited: process exits
  Exited --> Restarted: restart policy
  Restarted --> Start
  Exited --> [*]
```

### CI/CD

```mermaid
flowchart LR
  Push["Git push or PR"] --> Actions["GitHub Actions"]
  Actions --> Setup["Set up Java 21"]
  Setup --> Test["mvn test"]
  Test --> ComposeConfig["docker compose config"]
  ComposeConfig --> Images["Build service images"]
  Images --> Review["Ready for deploy"]
```

### Service Discovery

```mermaid
flowchart LR
  ServiceName["risk-service"] --> DNSRecord["Cluster DNS A record"]
  DNSRecord --> ClusterIP["Stable virtual IP"]
  ClusterIP --> Endpoints["EndpointSlice"]
  Endpoints --> PodA["risk-service Pod A"]
  Endpoints --> PodB["risk-service Pod B"]
```
