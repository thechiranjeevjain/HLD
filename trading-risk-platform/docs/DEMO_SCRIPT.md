# Trading Risk Platform Demo Script

## Fast Demo: Standalone Pre-Trade Engine

```powershell
cd G:\TechStudyNotes\SystemDesignProjects\trading-risk-platform
mvn -pl pretrade-risk-engine spring-boot:run
```

Open `http://localhost:8090`.

## Walkthrough

1. Run accept and reject scenarios from the dashboard.
2. Submit a FIX message and show parsed fields.
3. Run the race scenario and explain atomic reservation.
4. Enable the account kill switch and show immediate rejection.
5. Run the failure scenario and explain fail-closed behavior.
6. Run the PnL scenario and show how fills plus market ticks change state.

## API Demo

```powershell
Invoke-RestMethod -Method Post `
  -Uri http://localhost:8090/api/orders `
  -ContentType 'application/json' `
  -Body '{"clOrdId":"JSON-1001","account":"ACCT-DEMO","symbol":"MSFT","side":"BUY","quantity":100,"price":410.25,"autoFill":false}'

Invoke-RestMethod -Method Post http://localhost:8090/api/scenarios/race
Invoke-RestMethod -Method Post http://localhost:8090/api/scenarios/failure
```

## Microservice Demo

```powershell
docker compose up --build
```

Then submit through the gateway at `http://localhost:8084/api/orders`.

## Interview Close

Say: this project shows how risk systems trade latency, safety, and operability. The safest behavior under uncertain state is to reject, not accept silently.
