# Ride-Sharing Backend Demo Script

## Start

```powershell
cd G:\TechStudyNotes\SystemDesignProjects\ride-sharing-backend
docker compose up --build
```

API base URL: `http://localhost:8086`.

## Walkthrough

1. Register a driver.

```powershell
$driver = Invoke-RestMethod -Method Post `
  -Uri http://localhost:8086/api/drivers `
  -ContentType 'application/json' `
  -Body '{"name":"Driver One","latitude":40.758,"longitude":-73.9855}'
```

2. Search nearby drivers.

```powershell
Invoke-RestMethod "http://localhost:8086/api/drivers/nearby?latitude=40.758&longitude=-73.9855&radiusKm=5"
```

3. Request a ride.

```powershell
$ride = Invoke-RestMethod -Method Post `
  -Uri http://localhost:8086/api/rides `
  -ContentType 'application/json' `
  -Body '{"riderId":"rider-1","pickupLatitude":40.758,"pickupLongitude":-73.9855,"dropoffLatitude":40.7306,"dropoffLongitude":-73.9352,"radiusKm":5}'
```

4. Move the ride state.

```powershell
Invoke-RestMethod -Method Patch `
  -Uri "http://localhost:8086/api/rides/$($ride.id)/status" `
  -ContentType 'application/json' `
  -Body '{"status":"STARTED"}'
```

## Interview Close

Say: the heart of this project is matching under state constraints: driver availability, nearby search, and ride lifecycle correctness.
