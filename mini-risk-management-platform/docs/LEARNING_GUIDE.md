# Learning Guide

Use this project as a multi-week production engineering bootcamp.

## Week 1: Java Process And Spring Request Lifecycle

Read:

- `api-gateway/src/main/java/.../GatewayController.java`
- `order-service/src/main/java/.../OrderController.java`
- `order-service/src/main/java/.../OrderApplicationService.java`

Be able to explain:

- What a JVM process is.
- How Spring maps HTTP requests to controller methods.
- Why validation happens at the edge.
- Why downstream calls need timeouts.

## Week 2: Databases And Ownership

Read:

- `order-service/src/main/resources/db/migration`
- `risk-service/src/main/resources/db/migration`
- `history-service/src/main/resources/db/migration`
- `docs/ARCHITECTURE.md`

Be able to explain:

- Why each service has its own schema.
- How Flyway works.
- Why JPA entities are not shared across services.
- How indexes support query paths.

## Week 3: Kafka And Asynchronous Systems

Read:

- `order-service/.../OrderEventPublisher.java`
- `history-service/.../OrderEventConsumer.java`
- `notification-service/.../OrderNotificationConsumer.java`

Be able to explain:

- Topics, partitions, producers, consumers, and consumer groups.
- At-least-once delivery.
- Why consumers must be idempotent.
- What consumer lag means.

## Week 4: Redis And Caching

Read:

- `risk-service/.../RiskLimitLookup.java`

Be able to explain:

- Cache hit, miss, TTL, and fallback.
- Why Redis failure should not necessarily fail risk checks.
- Why cache invalidation is hard.
- Difference between cache-aside and write-through.

## Week 5: Docker And Linux Internals

Read:

- `docs/DOCKER_GUIDE.md`
- `docs/LINUX_INTERNALS.md`
- all service `Dockerfile`s

Be able to explain:

- Linux processes, namespaces, cgroups, and OverlayFS.
- How a container starts.
- How bridge networking works.
- Why containers are not VMs.

## Week 6: Kubernetes

Read:

- `docs/KUBERNETES_GUIDE.md`
- `k8s/base`
- `k8s/apps`
- `k8s/data`

Be able to explain:

- Pod lifecycle.
- Deployment rollout.
- Service discovery.
- Ingress flow.
- Resource requests and limits.
- Liveness versus readiness.

## Week 7: Production Debugging

Read:

- `docs/DEBUGGING_GUIDE.md`
- `docs/PRODUCTION_INCIDENTS.md`
- `docs/HANDS_ON_LABS.md`

Practice:

- Break a ConfigMap.
- Break a Service selector.
- Delete Pods.
- Simulate DB and Kafka failures.
- Inspect logs, events, endpoints, and metrics.

## Week 8: Senior Interview Mode

Read:

- `docs/INTERVIEW_GUIDE.md`
- `docs/PRODUCTION_GUIDE.md`

Practice answering:

- What did you build?
- Why this architecture?
- What fails first under load?
- How do you prevent event loss?
- How would you make this platform multi-region?
- How do Docker and Kubernetes actually work under Linux?

