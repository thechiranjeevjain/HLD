# Fraud Detection Platform Demo Script

## Start

```powershell
cd G:\TechStudyNotes\SystemDesignProjects\fraud-detection-platform
docker compose up --build
```

API base URL: `http://localhost:8087`.

## Walkthrough

1. Score a normal transaction.

```powershell
Invoke-RestMethod -Method Post `
  -Uri http://localhost:8087/api/events/transactions `
  -ContentType 'application/json' `
  -Body '{"transactionId":"txn-1001","userId":"user-1","amount":125.50,"currency":"USD","merchantCategory":"GROCERY","country":"US","homeCountry":"US","cardPresent":true}'
```

2. Score a high-risk transaction.

```powershell
Invoke-RestMethod -Method Post `
  -Uri http://localhost:8087/api/events/transactions `
  -ContentType 'application/json' `
  -Body '{"transactionId":"txn-9001","userId":"user-2","amount":6200.00,"currency":"USD","merchantCategory":"CRYPTO","country":"SG","homeCountry":"US","cardPresent":false}'
```

3. Read the stored decision.

```powershell
Invoke-RestMethod http://localhost:8087/api/risks/txn-9001
```

## Interview Close

Say: fraud decisions need speed, idempotency, explainability, and auditability. This project keeps those boundaries visible.
