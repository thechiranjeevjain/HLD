# Authentication Service Demo Script

## Start

```powershell
cd G:\TechStudyNotes\SystemDesignProjects\authentication-service
docker compose up --build
```

API base URL: `http://localhost:8081`.

## Walkthrough

1. Show health.

```powershell
Invoke-RestMethod http://localhost:8081/actuator/health
```

2. Register a user and store the token.

```powershell
$auth = Invoke-RestMethod -Method Post `
  -Uri http://localhost:8081/api/auth/register `
  -ContentType 'application/json' `
  -Body '{"email":"user@example.com","password":"UserPass123!","displayName":"Demo User"}'
```

3. Use the token against a protected route.

```powershell
Invoke-RestMethod `
  -Uri http://localhost:8081/api/users/me `
  -Headers @{ Authorization = "Bearer $($auth.accessToken)" }
```

4. Show a mock federated login.

```powershell
Invoke-RestMethod -Method Post `
  -Uri http://localhost:8081/api/auth/oauth2/mock `
  -ContentType 'application/json' `
  -Body '{"provider":"github","providerSubject":"github-123","email":"oauth@example.com","displayName":"OAuth User"}'
```

## Interview Close

Say: this project is about identity boundaries: credential verification, token issue, token validation, and role enforcement.
