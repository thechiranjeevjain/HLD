# Hotel Booking Service Demo Script

## Start

```powershell
cd G:\TechStudyNotes\SystemDesignProjects\hotel-booking-service
mvn spring-boot:run
```

App URL: `http://localhost:8080`.

## Walkthrough

1. Prepare Basic auth headers.

```powershell
$user = @{ Authorization = "Basic " + [Convert]::ToBase64String([Text.Encoding]::ASCII.GetBytes("user:password")) }
$admin = @{ Authorization = "Basic " + [Convert]::ToBase64String([Text.Encoding]::ASCII.GetBytes("admin:admin")) }
```

2. Show API health and contract.

```powershell
Invoke-RestMethod http://localhost:8080/actuator/health
Invoke-WebRequest http://localhost:8080/openapi.yaml
```

3. Read and search hotels.

```powershell
Invoke-RestMethod http://localhost:8080/hotel/1 -Headers $user
Invoke-RestMethod http://localhost:8080/search/1 -Headers $user
```

4. Show admin-only delete.

```powershell
Invoke-RestMethod -Method Delete http://localhost:8080/hotel/4 -Headers $admin
```

## Interview Close

Say: this project demonstrates the path from a simple API to an operable service: auth, API contract, cache, events, metrics, traces, Docker, and Kubernetes.
