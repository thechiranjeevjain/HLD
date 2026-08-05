# Docker Guide

## Why Docker Exists

Before containers, developers often said "works on my machine" because local libraries, OS packages, ports, and environment variables differed from production. Docker packages the application process with a filesystem, environment, network namespace, and resource controls so it behaves more consistently.

Docker does not virtualize a full machine. It runs Linux processes isolated by kernel features.

## How This Project Uses Docker

- Each service has a multi-stage `Dockerfile`.
- Maven builds run in a Java 21 build image.
- The runtime image contains only a JRE, app jar, non-root user, and `curl` for health checks.
- Docker Compose creates a bridge network named `risk-net`.
- Service names such as `risk-service` resolve through Docker DNS.
- Volumes persist PostgreSQL, Kafka, Redis, Prometheus, and Grafana data.

## Build Anatomy

```text
maven:3.9.9-eclipse-temurin-21
  -> copy poms
  -> dependency:go-offline
  -> copy source
  -> mvn package
  -> produce service jar

eclipse-temurin:21-jre-alpine
  -> install curl
  -> create non-root app user
  -> copy jar
  -> expose port
  -> healthcheck readiness endpoint
  -> java -jar app.jar
```

## Useful Commands

```powershell
docker compose ps
docker compose logs -f order-service
docker compose exec postgres psql -U risk -d orders -c "select * from orders;"
docker compose exec kafka /opt/kafka/bin/kafka-topics.sh --bootstrap-server localhost:9092 --list
docker compose exec redis redis-cli keys "*"
docker inspect mini-risk-management-platform-order-service-1
docker network inspect mini-risk-management-platform_risk-net
docker volume ls
```

## What Happens Internally

1. Docker asks the daemon to create containers.
2. The daemon prepares a writable container layer over image layers using OverlayFS.
3. The daemon creates namespaces for PID, mount, network, IPC, UTS, and optionally user isolation.
4. The daemon applies cgroup limits.
5. The daemon connects containers to a Linux bridge network.
6. The container runtime starts the process using `runc`.
7. Docker watches process exit status and health check results.

## Common Failures

| Failure | Why it happens | Debug command |
| --- | --- | --- |
| Port already in use | Host port is occupied. | `netstat -ano \| findstr :8080` |
| Container exits immediately | Java process crashed or config missing. | `docker compose logs service-name` |
| DB connection refused | PostgreSQL not healthy or wrong URL. | `docker compose ps postgres` |
| DNS lookup fails | Wrong service name or network. | `docker compose exec order-service getent hosts risk-service` |
| Data disappeared | Volume deleted with `down -v`. | `docker volume ls` |

## Best Practices

- Use multi-stage builds.
- Run as non-root.
- Pin image tags for reproducibility.
- Use environment variables for deploy-time config.
- Add health checks, but avoid probes that are too aggressive.
- Persist state with named volumes.
- Keep application logs on stdout/stderr.

## Tradeoffs

- Containers are lighter than VMs, but they share the host kernel.
- Images improve reproducibility, but vulnerable base layers need patching.
- Compose is excellent for local labs, but it is not a production orchestrator.
- Health checks improve automation, but bad health checks cause false restarts.

