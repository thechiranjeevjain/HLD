$ErrorActionPreference = 'Stop'
$baseUrl = 'http://localhost:8080'

$newer = @{ updateId='demo-2'; sku='SKU-DEMO'; storeId='STORE-101'; quantity=25; version=2; eventTime='2026-08-16T10:02:00Z' } | ConvertTo-Json
$older = @{ updateId='demo-1'; sku='SKU-DEMO'; storeId='STORE-101'; quantity=10; version=1; eventTime='2026-08-16T10:01:00Z' } | ConvertTo-Json

Invoke-RestMethod -Method Post -Uri "$baseUrl/api/v1/inventory/updates" -ContentType 'application/json' -Body $newer
Invoke-RestMethod -Method Post -Uri "$baseUrl/api/v1/inventory/updates" -ContentType 'application/json' -Body $older
Invoke-RestMethod -Method Get -Uri "$baseUrl/api/v1/inventory/SKU-DEMO/summary" | ConvertTo-Json -Depth 5
