$ErrorActionPreference = 'Stop'

function Wait-Http([string]$Uri, [int]$Seconds = 180) {
    $deadline = (Get-Date).AddSeconds($Seconds)
    do {
        try {
            $response = Invoke-WebRequest -UseBasicParsing -Uri $Uri -TimeoutSec 3
            if ($response.StatusCode -lt 500) { return }
        } catch { Start-Sleep -Seconds 2 }
    } while ((Get-Date) -lt $deadline)
    throw "Timed out waiting for $Uri"
}

Write-Host 'Starting the complete local platform...'
docker compose up --build -d
Wait-Http 'http://localhost:8081/realms/orders/.well-known/openid-configuration'
Wait-Http 'http://localhost:8080/actuator/health/readiness'

$tokenResponse = Invoke-RestMethod -Method Post -Uri 'http://localhost:8081/realms/orders/protocol/openid-connect/token' -ContentType 'application/x-www-form-urlencoded' -Body @{
    client_id = 'order-cli'; username = 'alice'; password = 'alice'; grant_type = 'password'
}
$headers = @{ Authorization = "Bearer $($tokenResponse.access_token)"; 'Idempotency-Key' = "demo-$([guid]::NewGuid())" }
$order = Invoke-RestMethod -Method Post -Uri 'http://localhost:8080/api/v1/orders' -Headers $headers -ContentType 'application/json' -Body '{"sku":"CHAIR-42","quantity":2,"unitPrice":149.99}'
Write-Host "Created order $($order.id) in state $($order.status)"

$deadline = (Get-Date).AddSeconds(30)
do {
    Start-Sleep -Seconds 1
    $current = Invoke-RestMethod -Uri "http://localhost:8080/api/v1/orders/$($order.id)" -Headers @{ Authorization = "Bearer $($tokenResponse.access_token)" }
} while ($current.status -ne 'ACCEPTED' -and (Get-Date) -lt $deadline)
if ($current.status -ne 'ACCEPTED') { throw "Kafka fulfillment did not accept the order; final state: $($current.status)" }

$health = Invoke-RestMethod -Uri 'http://localhost:8080/actuator/health/readiness'
$metrics = Invoke-WebRequest -UseBasicParsing -Uri 'http://localhost:8080/actuator/prometheus'
if ($health.status -ne 'UP' -or $metrics.Content -notmatch 'orders_outbox_published_total') { throw 'Health or business metrics verification failed' }

Write-Host 'PASS: JWT authentication, PostgreSQL transaction, Redis-backed read, Kafka fulfillment, health, and metrics all worked.'
Write-Host 'Grafana: http://localhost:3000  Prometheus: http://localhost:9090'
