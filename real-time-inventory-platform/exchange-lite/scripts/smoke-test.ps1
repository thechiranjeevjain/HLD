param(
    [string]$SidecarUrl = "http://127.0.0.1:8080"
)

$ErrorActionPreference = "Stop"

$routes = @(
    "/health",
    "/stats",
    "/markets",
    "/sessions",
    "/risk",
    "/heap",
    "/threads",
    "/config"
)

foreach ($route in $routes) {
    $url = "$SidecarUrl$route"
    $response = Invoke-WebRequest -Uri $url -UseBasicParsing
    if ($response.StatusCode -ge 400) {
        throw "Smoke test failed for $url with status $($response.StatusCode)"
    }
    Write-Host "$route $($response.StatusCode)"
}
