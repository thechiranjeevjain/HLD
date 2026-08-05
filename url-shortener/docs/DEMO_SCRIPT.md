# URL Shortener Demo Script

## Start

```powershell
cd G:\TechStudyNotes\SystemDesignProjects\url-shortener
docker compose up --build
```

API base URL: `http://localhost:8082`.

## Walkthrough

1. Create a short link.

```powershell
$link = Invoke-RestMethod -Method Post `
  -Uri http://localhost:8082/api/links `
  -ContentType 'application/json' `
  -Body '{"originalUrl":"https://example.com/products/sku-1001","ownerKey":"demo-user"}'
```

2. Read metadata.

```powershell
Invoke-RestMethod "http://localhost:8082/api/links/$($link.code)"
```

3. Follow the redirect without auto-following it.

```powershell
Invoke-WebRequest "http://localhost:8082/$($link.code)" -MaximumRedirection 0
```

4. Explain the production upgrade.

Say: at scale, redirect lookup should be cached and analytics should move off the critical redirect path.
