# Project Structure

This project is intentionally explicit. A senior engineer should be able to open a folder, explain why it exists, and connect it to a production concern.

## Top-Level Files

| Path                       | Why it exists                                                                                                  |
| -------------------------- | -------------------------------------------------------------------------------------------------------------- |
| `pom.xml`                  | Parent Maven build. Locks Java 21, Spring Boot dependency management, and service modules.                     |
| `docker-compose.yml`       | Local production-like environment: apps, PostgreSQL, Kafka, Redis, Prometheus, Grafana, networks, and volumes. |
| `.github/workflows/ci.yml` | CI pipeline for Maven tests and Docker image build checks.                                                     |
| `.editorconfig`            | Consistent formatting across IDEs.                                                                             |
| `.gitignore`               | Prevents build output, local IDE files, and secrets from being committed.                                      |

## Service Folders

| Folder                 | Why it exists                                                                                             |
| ---------------------- | --------------------------------------------------------------------------------------------------------- |
| `shared`               | Shared API contracts used by REST and Kafka payloads. Keeps schemas visible and versionable.              |
| `api-gateway`          | Entry point for clients. Routes requests to internal services. In Kubernetes, Ingress sends traffic here. |
| `order-service`        | Owns order intake, order persistence, risk-service calls, and order event publishing.                     |
| `risk-service`         | Owns risk-limit decisions. Uses PostgreSQL as source of truth and Redis as cache.                         |
| `history-service`      | Owns exposure history and running totals from accepted order events.                                      |
| `notification-service` | Demonstrates asynchronous consumers and side effects without external email infrastructure.               |

## Common Service Layout

| Path                                 | Why it exists                                                                       |
| ------------------------------------ | ----------------------------------------------------------------------------------- |
| `src/main/java/.../api`              | REST controllers. Defines public HTTP surface of the service.                       |
| `src/main/java/.../service`          | Business workflows and rules. This is where interviewers should start for behavior. |
| `src/main/java/.../domain`           | JPA entities and domain objects owned by the service.                               |
| `src/main/java/.../repository`       | Persistence ports implemented by Spring Data JPA.                                   |
| `src/main/java/.../client`           | Downstream HTTP clients. Makes service dependencies explicit.                       |
| `src/main/java/.../messaging`        | Kafka producers and consumers.                                                      |
| `src/main/java/.../config`           | Framework wiring: clients, topics, serialization, and timeouts.                     |
| `src/main/resources/application.yml` | Environment-driven service configuration.                                           |
| `src/main/resources/db/migration`    | Flyway migrations. Database schema lives with the owning service.                   |
| `src/test/java`                      | Unit and opt-in Testcontainers tests.                                               |
| `Dockerfile`                         | Multi-stage build and runtime image for the service.                                |

## Infrastructure Folders

| Path                                     | Why it exists                                                      |
| ---------------------------------------- | ------------------------------------------------------------------ |
| `infra/docker/postgres/init.sql`         | Creates separate local databases for each stateful service.        |
| `infra/docker/prometheus/prometheus.yml` | Scrape configuration for Spring Actuator Prometheus endpoints.     |
| `infra/docker/grafana`                   | Datasource and dashboard provisioning for local Grafana.           |
| `k8s/base`                               | Namespace, shared ConfigMap, Secret, and Ingress.                  |
| `k8s/data`                               | PostgreSQL, Redis, and Kafka Deployments, Services, PVs, and PVCs. |
| `k8s/apps`                               | Application Deployments, Services, probes, resources, and HPAs.    |
| `k8s/observability`                      | Kubernetes Prometheus deployment and scrape config.                |
| `docs`                                   | Learning material, diagrams, incidents, labs, and interview guide. |
