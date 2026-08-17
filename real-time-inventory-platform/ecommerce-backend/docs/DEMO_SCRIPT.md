# E-Commerce Backend Demo Script

## Start

```powershell
cd G:\TechStudyNotes\SystemDesignProjects\ecommerce-backend
docker compose up --build
```

API base URL: `http://localhost:8083`.

## Walkthrough

1. Create inventory.

```powershell
Invoke-RestMethod -Method Post `
  -Uri http://localhost:8083/api/inventory `
  -ContentType 'application/json' `
  -Body '{"sku":"SKU-1001","name":"Mechanical Keyboard","price":129.99,"currency":"USD","stockQuantity":10}'
```

2. Place an order.

```powershell
$order = Invoke-RestMethod -Method Post `
  -Uri http://localhost:8083/api/orders `
  -ContentType 'application/json' `
  -Body '{"customerId":"customer-1","items":[{"sku":"SKU-1001","quantity":2}]}'
```

3. Capture payment.

```powershell
Invoke-RestMethod -Method Post `
  -Uri "http://localhost:8083/api/orders/$($order.id)/payments" `
  -ContentType 'application/json' `
  -Body '{"paymentToken":"tok_success_demo"}'
```

4. Show the failure path.

```powershell
Invoke-RestMethod -Method Post `
  -Uri "http://localhost:8083/api/orders/$($order.id)/payments" `
  -ContentType 'application/json' `
  -Body '{"paymentToken":"fail_demo"}'
```

## Interview Close

Say: the main lesson is where to draw the transaction boundary so stock, order state, and event publication do not drift silently.
