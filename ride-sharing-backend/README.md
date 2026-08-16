# Ride-Sharing Backend

A Spring Boot ride-sharing backend with driver location updates, nearby-driver geo queries, ride matching, trip lifecycle transitions, and WebSocket ride updates.

## Stack

- Java 17
- Spring Boot 3.3.6
- Spring Web
- Spring WebSocket/STOMP
- Spring Data JPA
- Flyway
- PostgreSQL
- Docker Compose

## Endpoints

| Method  | Path                         | Purpose                                 |
| ------- | ---------------------------- | --------------------------------------- |
| `POST`  | `/api/drivers`               | Register a driver                       |
| `PATCH` | `/api/drivers/{id}/location` | Update driver location and availability |
| `GET`   | `/api/drivers/nearby`        | Search available drivers by radius      |
| `POST`  | `/api/rides`                 | Request a ride and match nearest driver |
| `GET`   | `/api/rides/{id}`            | Read trip status                        |
| `PATCH` | `/api/rides/{id}/status`     | Move trip through lifecycle             |
| `GET`   | `/actuator/health`           | Health check                            |

WebSocket clients can subscribe to `/topic/rides/{rideId}` after connecting to `/ws`.

## Run

```powershell
docker compose up --build
```

The API listens on `http://localhost:8086`.

## Smoke Test

Create a driver:

```powershell
$driver = Invoke-RestMethod -Method Post `
  -Uri http://localhost:8086/api/drivers `
  -ContentType 'application/json' `
  -Body '{
    "name": "Driver One",
    "latitude": 40.758,
    "longitude": -73.9855
  }'
```

Find nearby drivers:

```powershell
Invoke-RestMethod "http://localhost:8086/api/drivers/nearby?latitude=40.758&longitude=-73.9855&radiusKm=5"
```

Request a ride:

```powershell
$ride = Invoke-RestMethod -Method Post `
  -Uri http://localhost:8086/api/rides `
  -ContentType 'application/json' `
  -Body '{
    "riderId": "rider-1",
    "pickupLatitude": 40.758,
    "pickupLongitude": -73.9855,
    "dropoffLatitude": 40.7306,
    "dropoffLongitude": -73.9352,
    "radiusKm": 5
  }'

$ride
```

Start the trip:

```powershell
Invoke-RestMethod -Method Patch `
  -Uri "http://localhost:8086/api/rides/$($ride.id)/status" `
  -ContentType 'application/json' `
  -Body '{ "status": "STARTED" }'
```

## Verify

```powershell
mvn clean verify
```
