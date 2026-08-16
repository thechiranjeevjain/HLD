$base = "http://127.0.0.1:8080"
$event = @{
  idempotencyKey = "demo-out-for-delivery-001"
  carrier = "UPS"
  trackingNumber = "1Z-DEMO-001"
  eventType = "OUT_FOR_DELIVERY"
  eventTime = (Get-Date).ToUniversalTime().ToString("o")
  location = "Portland, OR"
  rawPayload = '{"carrier_status":"OFD","facility":"PDX"}'
} | ConvertTo-Json
Invoke-RestMethod -Method Post -Uri "$base/carrier/events" -ContentType "application/json" -Body $event
Invoke-RestMethod -Uri "$base/orders/ORD-1001/tracking" -Headers @{"X-Actor-Id"="user-123"} | ConvertTo-Json -Depth 8
