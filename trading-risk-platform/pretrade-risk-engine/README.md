# PTR-Inspired Pre-Trade Risk Engine

A compact Java 21/Spring Boot simulation of a low-latency pre-trade risk system. Orders enter through an in-process DSF-like message bus, **not REST**. One event-loop owns mutable risk state; the REST control plane only manages versioned configuration and operations.

## Choose a Track

| Goal                                      | Start here                                                      |
| ----------------------------------------- | --------------------------------------------------------------- |
| Prepare the 40–60 minute interview answer | [40-60_MINUTE_HLD.md](40-60_MINUTE_HLD.md)                      |
| Understand the implementation             | [INTERVIEW_GUIDE.md](INTERVIEW_GUIDE.md) and [DEMO.md](DEMO.md) |
| Evaluate real deployment gaps             | [PRODUCTION_READINESS.md](PRODUCTION_READINESS.md)              |

The codebase is shared; the evidence and completion criteria are separate.

## Run

```powershell
$env:JAVA_HOME='C:\Program Files\Eclipse Adoptium\jdk-21.0.12.8-hotspot' # or any JDK 21+
mvn -pl pretrade-risk-engine -am test
mvn -pl pretrade-risk-engine spring-boot:run -Dspring-boot.run.profiles=demo
```

Or use reproducible Java 21 containers:

```powershell
cd pretrade-risk-engine
docker compose up --build
Invoke-RestMethod http://localhost:8091/runtime
Invoke-WebRequest http://localhost:8090/actuator/prometheus
```

Grafana is at `http://localhost:3000` (admin/admin), Prometheus at `http://localhost:9090`, engine health at `http://localhost:8090/actuator/health`, and the NFF-like sidecar at `http://localhost:8091/runtime`.

## Important boundaries

- `PtrRuntime.submit(Order)` represents DSF ingress. It is intentionally not a controller method.
- `POST /api/config` and `GET /api/operations/runtime` are JWT/RBAC-protected control-plane operations.
- `/api/internal/runtime` is pod-internal input for the sidecar in this local simulation; Kubernetes exposes only the sidecar service.
- The event bus, journal, lease store, and configuration distribution are local substitutes for proprietary infrastructure.

See the track table above rather than treating local runnable proof as a production-readiness claim.
