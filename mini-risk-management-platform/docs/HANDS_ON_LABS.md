# Hands-On Labs

Run these labs in order. Each lab is designed to teach one production concept through a concrete break/fix cycle.

## Lab 1: Run Everything

Goal: start the full local stack.

Theory: Docker Compose creates containers, a bridge network, named volumes, and health checks.

Commands:

```powershell
docker compose up --build
docker compose ps
```

Expected output: all application services become healthy; Prometheus and Grafana are reachable.

What happened internally: Docker built images, created container namespaces, attached containers to a bridge network, and mounted named volumes.

Interview discussion: explain why service names work inside Compose but not automatically from the host.

## Lab 2: Inspect Docker

Goal: inspect containers, networks, and volumes.

Theory: containers are Linux processes with isolated namespaces and mounted filesystems.

Commands:

```powershell
docker compose ps
docker inspect mini-risk-management-platform-order-service-1
docker network inspect mini-risk-management-platform_risk-net
docker volume ls
```

Expected output: service containers on one bridge network and named volumes for stateful systems.

What happened internally: Docker stored desired container config and Linux created namespaces, veth pairs, and mounts.

Interview discussion: explain container versus image.

## Lab 3: Inspect Kubernetes

Goal: deploy and inspect Kubernetes resources.

Theory: Kubernetes stores desired state and controllers reconcile it.

Commands:

```powershell
kubectl apply -k k8s
kubectl -n mini-risk get pods,svc,hpa,pv,pvc
kubectl -n mini-risk describe deployment order-service
```

Expected output: Deployments create Pods; Services expose stable DNS names; PVCs bind to PVs.

What happened internally: API server accepted YAML, scheduler assigned Pods, kubelet started containers.

Interview discussion: explain Deployment -> ReplicaSet -> Pod.

## Lab 4: Break Networking

Goal: see how a wrong service URL fails.

Theory: service discovery depends on DNS names and ports.

Commands:

```powershell
kubectl -n mini-risk set env deployment/order-service RISK_SERVICE_URL=http://risk-service-wrong:8082
kubectl -n mini-risk rollout status deployment/order-service
kubectl -n mini-risk logs deployment/order-service --tail=50
```

Expected output: orders are rejected with risk-service unavailable.

What happened internally: DNS lookup or TCP connection failed before `risk-service` received the request.

Fix:

```powershell
kubectl -n mini-risk set env deployment/order-service RISK_SERVICE_URL=http://risk-service:8082
```

Interview discussion: explain DNS failure versus Service endpoint failure.

## Lab 5: Break Volumes

Goal: understand persistence and data loss risk.

Theory: database files must live outside ephemeral container layers.

Commands:

```powershell
docker compose down -v
docker compose up --build
```

Expected output: previous orders and exposure history are gone.

What happened internally: named volumes were deleted, so PostgreSQL initialized fresh data files.

Interview discussion: explain why deleting a Pod should not delete database data.

## Lab 6: Break ConfigMaps

Goal: observe bad non-secret configuration.

Theory: ConfigMaps inject runtime values; Pods must restart or reload to pick up changes depending on injection method.

Commands:

```powershell
kubectl -n mini-risk patch configmap mini-risk-config --type merge -p '{"data":{"HISTORY_SERVICE_URL":"http://history-service-bad:8083"}}'
kubectl -n mini-risk rollout restart deployment/risk-service
kubectl -n mini-risk logs deployment/risk-service --tail=50
```

Expected output: risk checks reject with history unavailable.

What happened internally: new Pods received bad env vars from ConfigMap.

Fix:

```powershell
kubectl -n mini-risk patch configmap mini-risk-config --type merge -p '{"data":{"HISTORY_SERVICE_URL":"http://history-service:8083"}}'
kubectl -n mini-risk rollout restart deployment/risk-service
```

Interview discussion: explain ConfigMap as env vars versus mounted files.

## Lab 7: Break Secrets

Goal: see how wrong credentials appear.

Theory: Secrets inject sensitive values; bad DB credentials make services unready or crash.

Commands:

```powershell
kubectl -n mini-risk patch secret mini-risk-secrets --type merge -p '{"stringData":{"SPRING_DATASOURCE_PASSWORD":"wrong"}}'
kubectl -n mini-risk rollout restart deployment/order-service
kubectl -n mini-risk logs deployment/order-service --tail=100
```

Expected output: datasource authentication failure and readiness failure.

What happened internally: HikariCP cannot create database connections.

Fix: restore the Secret value to `risk` and restart affected deployments.

Interview discussion: explain why base64 is not encryption.

## Lab 8: Delete Pods

Goal: observe self-healing.

Theory: Deployments maintain desired replica count.

Commands:

```powershell
kubectl -n mini-risk delete pod -l app=order-service
kubectl -n mini-risk get pods -w
```

Expected output: old Pods terminate and new Pods appear.

What happened internally: ReplicaSet controller noticed missing replicas and created replacements.

Interview discussion: explain why deleting a Pod is not the same as deleting a Deployment.

## Lab 9: Scale Deployments

Goal: scale stateless services.

Theory: multiple Pods can serve the same API when state is externalized.

Commands:

```powershell
kubectl -n mini-risk scale deployment/order-service --replicas=4
kubectl -n mini-risk get pods -l app=order-service
kubectl -n mini-risk get endpointslice -l kubernetes.io/service-name=order-service
```

Expected output: four Pods and multiple endpoints behind the Service.

What happened internally: Service load balances to ready endpoints.

Interview discussion: explain what would break if local in-memory state were required.

## Lab 10: Simulate Production Outage

Goal: walk a full outage investigation.

Theory: production debugging identifies failing boundaries before changing systems.

Commands:

```powershell
kubectl -n mini-risk scale deployment/postgres --replicas=0
kubectl -n mini-risk get pods
kubectl -n mini-risk logs deployment/order-service --tail=100
kubectl -n mini-risk describe pod -l app=order-service
kubectl -n mini-risk get events --sort-by=.lastTimestamp
```

Expected output: DB-dependent services become unhealthy or return errors.

What happened internally: JDBC connections fail; readiness eventually removes Pods from Service endpoints.

Fix:

```powershell
kubectl -n mini-risk scale deployment/postgres --replicas=1
kubectl -n mini-risk rollout status deployment/postgres
kubectl -n mini-risk rollout restart deployment/order-service deployment/risk-service deployment/history-service
```

Interview discussion: explain graceful degradation, dependency health, and fail-closed risk behavior.

