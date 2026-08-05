# Mini Risk Management Platform Demo Script

## Local Constraint

This project targets Java 21. On this workstation, previous local verification used the installed IntelliJ JDK 25 with the Byte Buddy experimental flag until a real JDK 21 is installed.

## Test

```powershell
cd G:\TechStudyNotes\SystemDesignProjects\mini-risk-management-platform
$jdk = "C:\Users\Chiranjeev Jain\.jdks\openjdk-25"
$env:JAVA_HOME = $jdk
$env:Path = "$jdk\bin;$env:Path"
mvn "-Dnet.bytebuddy.experimental=true" test
```

## Docker Compose Demo

```powershell
docker compose up --build
```

After services are healthy, submit an order through the gateway:

```powershell
Invoke-RestMethod -Method Post `
  -Uri http://localhost:8080/api/orders `
  -ContentType 'application/json' `
  -Body '{"clientId":"CLIENT-A","symbol":"MSFT","side":"BUY","quantity":100,"price":410.25}'
```

## Kubernetes Demo

```powershell
kubectl apply -k k8s
kubectl get pods -n risk-platform -w
```

Use this only after Docker Desktop or another cluster context is actually running.

## Talk Track

1. Start with `docs/DIAGRAMS.md`.
2. Explain gateway, order, risk, history, notification, PostgreSQL, Kafka, and Redis.
3. Show `docs/HANDS_ON_LABS.md` for failure drills.
4. Use `docs/INTERVIEW_GUIDE.md` for deeper Java, Docker, Kubernetes, and observability answers.
