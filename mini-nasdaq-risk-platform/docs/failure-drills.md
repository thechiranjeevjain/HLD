# Failure Drills

Run these after the platform is healthy. The goal is to practice observing the blast radius, reading logs, and restoring service.

## Kubernetes

Start with:

```powershell
kubectl get pods -n mini-risk
kubectl logs -n mini-risk deploy/order-service
```

1. Kill risk pods and confirm fail-closed behavior.

```powershell
kubectl delete pod -n mini-risk -l app=risk-service
```

Immediately submit an order through `/api/orders`. While risk pods are unavailable, `order-service` should reject the order with a fail-closed reason and publish an alert.

2. Break a risk limit config.

```powershell
kubectl patch configmap risk-platform-config -n mini-risk --type merge -p '{"data":{"RISK_MAX_ORDER_NOTIONAL":"100"}}'
kubectl rollout restart deployment/risk-service -n mini-risk
```

Submit a normal order and confirm it is rejected. Restore `RISK_MAX_ORDER_NOTIONAL` to `500000` and restart `risk-service`.

3. Exhaust memory on risk-service.

```powershell
kubectl set resources deployment/risk-service -n mini-risk --limits=memory=64Mi
kubectl rollout restart deployment/risk-service -n mini-risk
kubectl describe pod -n mini-risk -l app=risk-service
```

Look for restarts or OOMKilled status. Restore with:

```powershell
kubectl set resources deployment/risk-service -n mini-risk --limits=memory=256Mi
kubectl rollout restart deployment/risk-service -n mini-risk
```

4. Remove persistent storage.

```powershell
kubectl scale statefulset/postgres -n mini-risk --replicas=0
kubectl delete pvc -n mini-risk postgres-data-postgres-0
kubectl scale statefulset/postgres -n mini-risk --replicas=1
kubectl rollout restart deployment/order-service deployment/history-service -n mini-risk
```

Historical exposures should reset because the volume was deleted. This is the point of the drill.

5. Scale replicas and watch service routing.

```powershell
kubectl scale deployment/order-service deployment/risk-service -n mini-risk --replicas=3
kubectl get endpoints -n mini-risk order-service risk-service
```

Submit several orders and inspect which pods handled them through logs.

## Docker Compose

Useful local commands:

```powershell
docker compose ps
docker compose logs -f risk-service
docker compose restart risk-service
docker compose stop history-service
docker compose down
docker compose down -v
```

`docker compose down -v` removes the PostgreSQL volume and should wipe exposures.
