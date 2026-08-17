# Product Catalog API Demo Script

## Start

```powershell
cd G:\TechStudyNotes\SystemDesignProjects\product-catalog-api
docker compose up --build
```

API base URL: `http://localhost:8080`.

## Walkthrough

1. Check health.

```powershell
Invoke-RestMethod http://localhost:8080/actuator/health
```

2. Create a product.

```powershell
$product = Invoke-RestMethod -Method Post `
  -Uri http://localhost:8080/api/products `
  -ContentType 'application/json' `
  -Body '{"sku":"SKU-1001","name":"Mechanical Keyboard","description":"Hot-swappable keyboard","price":129.99,"currency":"USD","stockQuantity":25,"active":true}'
```

3. Search and filter.

```powershell
Invoke-RestMethod "http://localhost:8080/api/products?search=keyboard&active=true"
```

4. Update and delete.

```powershell
Invoke-RestMethod -Method Put `
  -Uri "http://localhost:8080/api/products/$($product.id)" `
  -ContentType 'application/json' `
  -Body '{"sku":"SKU-1001","name":"Mechanical Keyboard Pro","description":"Updated model","price":149.99,"currency":"USD","stockQuantity":15,"active":true}'

Invoke-RestMethod -Method Delete "http://localhost:8080/api/products/$($product.id)"
```

## Interview Close

Say: this small API demonstrates real backend fundamentals: validation, persistence, uniqueness, migrations, and predictable error contracts.
