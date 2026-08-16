# Debugging Guide

Production debugging is controlled narrowing. Do not jump randomly between logs, dashboards, and YAML. Start from the symptom, find the failing boundary, then prove the root cause.

## Golden Path

1. Reproduce the request.
2. Check gateway status.
3. Check downstream service status.
4. Check logs for the exact correlation window.
5. Check dependency health: PostgreSQL, Kafka, Redis.
6. Check networking and DNS.
7. Check resource pressure.
8. Check recent deploys and config changes.
9. Apply the smallest fix.
10. Add prevention: alert, test, runbook, validation, or safer default.

## Docker Debug Commands

```powershell
docker compose ps
docker compose logs -f api-gateway
docker compose logs -f order-service
docker compose exec order-service env
docker compose exec order-service sh
docker compose exec postgres psql -U risk -d orders -c "select count(*) from orders;"
docker compose exec kafka /opt/kafka/bin/kafka-consumer-groups.sh --bootstrap-server localhost:9092 --all-groups --describe
docker compose exec redis redis-cli info memory
docker network inspect mini-risk-management-platform_risk-net
docker volume inspect mini-risk-management-platform_postgres-data
```

## Kubernetes Debug Commands

```powershell
kubectl -n mini-risk get pods -o wide
kubectl -n mini-risk describe pod <pod-name>
kubectl -n mini-risk logs deployment/order-service
kubectl -n mini-risk logs deployment/risk-service --previous
kubectl -n mini-risk get events --sort-by=.lastTimestamp
kubectl -n mini-risk get svc,endpointslice
kubectl -n mini-risk exec deployment/order-service -- printenv
kubectl -n mini-risk exec deployment/order-service -- sh -c "wget -qO- http://risk-service:8082/actuator/health"
kubectl -n mini-risk top pods
kubectl -n mini-risk rollout history deployment/order-service
```

## Symptom To Boundary

| Symptom                                 | First boundary to check                            |
| --------------------------------------- | -------------------------------------------------- |
| Client gets 503                         | `api-gateway` downstream call.                     |
| Order rejected with risk unavailable    | `order-service` to `risk-service` HTTP path.       |
| Order rejected with history unavailable | `risk-service` to `history-service` HTTP path.     |
| Exposure not changing                   | Kafka event path and `history-service` consumer.   |
| Notifications missing                   | Kafka consumer group and notification logs.        |
| Pod keeps restarting                    | liveness probe, JVM crash, OOMKilled, bad config.  |
| Service has no traffic                  | Service selector and readiness endpoints.          |
| Ingress returns 404                     | Ingress class, host header, path, backend Service. |

## Debugging A Failed Order

1. Submit order and save response.
2. Check gateway logs.
3. Check order logs.
4. Check risk logs.
5. Check order DB row.
6. Check Kafka topic.
7. Check history DB row after consumer delay.

Commands:

```powershell
docker compose logs --tail=100 api-gateway
docker compose logs --tail=100 order-service
docker compose logs --tail=100 risk-service
docker compose exec postgres psql -U risk -d orders -c "select id, client_id, symbol, status, reason from orders order by created_at desc limit 5;"
docker compose exec kafka /opt/kafka/bin/kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic order-events --from-beginning --max-messages 5
docker compose exec postgres psql -U risk -d history -c "select order_id, client_id, symbol, quantity from exposure_events order by occurred_at desc limit 5;"
```

## Debugging Slow Requests

Check:

- HTTP latency in Prometheus.
- JVM CPU and heap.
- PostgreSQL slow queries.
- Connection pool usage.
- Kafka consumer lag.
- DNS and network timeouts.

Prometheus examples:

```promql
sum by (uri, status) (rate(http_server_requests_seconds_count[1m]))
histogram_quantile(0.95, sum by (le, uri) (rate(http_server_requests_seconds_bucket[5m])))
jvm_memory_used_bytes{area="heap"}
process_cpu_usage
```

## Debugging Resource Pressure

Kubernetes:

```powershell
kubectl -n mini-risk top pods
kubectl -n mini-risk describe pod <pod-name>
kubectl -n mini-risk get events --field-selector reason=OOMKilling
```

Docker:

```powershell
docker stats
docker inspect <container>
```

Expected observations:

- CPU throttling shows latency without restarts.
- Memory limit breach shows OOMKilled or exit code 137.
- Disk pressure shows write failures and node events.

## Debugging Networking

Docker:

```powershell
docker compose exec api-gateway sh -c "getent hosts order-service"
docker compose exec api-gateway sh -c "curl -v http://order-service:8081/actuator/health"
```

Kubernetes:

```powershell
kubectl -n mini-risk get svc,endpointslice
kubectl -n mini-risk exec deployment/api-gateway -- nslookup order-service
kubectl -n mini-risk exec deployment/api-gateway -- wget -S -O- http://order-service:8081/actuator/health
```

Interpretation:

- DNS fails: CoreDNS or wrong name.
- DNS succeeds but connect fails: Service, endpoints, NetworkPolicy, or port.
- Connect succeeds but HTTP 500: application or dependency.
