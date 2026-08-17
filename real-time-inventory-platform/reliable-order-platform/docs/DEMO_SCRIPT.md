# Reliable Order Platform Demo Script

## Verify

Use a JDK that supports release 21:

```powershell
cd G:\TechStudyNotes\SystemDesignProjects\reliable-order-platform
$jdk = "C:\Users\Chiranjeev Jain\.jdks\openjdk-25"
$env:JAVA_HOME = $jdk
$env:Path = "$jdk\bin;$env:Path"
.\mvnw.cmd verify
```

## Run Full Local Stack

Docker Desktop with Linux containers is required:

```powershell
cd G:\TechStudyNotes\SystemDesignProjects\reliable-order-platform
docker compose up --build
```

Quick demo:

```powershell
.\scripts\demo.ps1
```

## Walkthrough

1. Request a local development JWT.
2. Create an order with an `Idempotency-Key`.
3. Retry the same request and show that it converges on the same order.
4. Show the outbox row and explain the post-commit publish boundary.
5. Show the fulfillment consumer changing order state after Kafka delivery.
6. Re-deliver or rerun the consumer path and explain processed-event dedupe.
7. Open health/metrics and explain the cache-aside policy.

## Interview Close

Say: the system does not promise impossible exactly-once delivery. It makes retries safe at the API boundary and at the Kafka consumer boundary, while PostgreSQL owns the transactionally correct state.
