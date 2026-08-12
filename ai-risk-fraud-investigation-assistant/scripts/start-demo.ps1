$ErrorActionPreference = "Stop"
$projectDir = Split-Path -Parent $PSScriptRoot
$frontendDir = Join-Path $projectDir "frontend"
$targetDir = Join-Path $projectDir "target"
$pidFile = Join-Path $targetDir "demo-processes.json"

$jdk = Get-ChildItem "C:\Program Files\Eclipse Adoptium" -Directory -Filter "jdk-21*" |
    Sort-Object Name -Descending |
    Select-Object -First 1
if (-not $jdk) { throw "Java 21 is required. Install Eclipse Temurin 21 first." }

$env:JAVA_HOME = $jdk.FullName
$env:Path = "$($jdk.FullName)\bin;$env:Path"
New-Item -ItemType Directory -Force -Path $targetDir | Out-Null

Push-Location $projectDir
try {
    & mvn.cmd -q -DskipTests package
    if ($LASTEXITCODE -ne 0) { throw "Backend build failed." }
} finally { Pop-Location }
if (-not (Test-Path -LiteralPath (Join-Path $frontendDir "node_modules\.bin\vite.cmd"))) {
    Push-Location $frontendDir
    try {
        & npm.cmd ci
        if ($LASTEXITCODE -ne 0) { throw "Frontend dependency installation failed." }
    } finally { Pop-Location }
}

$backend = Start-Process -FilePath (Join-Path $jdk.FullName "bin\java.exe") `
    -ArgumentList "-jar", (Join-Path $targetDir "ai-risk-fraud-assistant-1.0.0.jar") `
    -WorkingDirectory $projectDir -WindowStyle Hidden -PassThru `
    -RedirectStandardOutput (Join-Path $targetDir "demo-backend.log") `
    -RedirectStandardError (Join-Path $targetDir "demo-backend-error.log")
$frontend = Start-Process -FilePath "npm.cmd" -ArgumentList "run", "dev", "--", "--host", "0.0.0.0" `
    -WorkingDirectory $frontendDir -WindowStyle Hidden -PassThru `
    -RedirectStandardOutput (Join-Path $targetDir "demo-frontend.log") `
    -RedirectStandardError (Join-Path $targetDir "demo-frontend-error.log")

@{ backend = $backend.Id; frontend = $frontend.Id } | ConvertTo-Json | Set-Content -LiteralPath $pidFile

$backendReady = $false
$frontendReady = $false
for ($attempt = 0; $attempt -lt 60; $attempt++) {
    try {
        if ((Invoke-RestMethod "http://localhost:8080/actuator/health" -TimeoutSec 2).status -eq "UP") { $backendReady = $true }
    } catch { }
    try {
        $client = [Net.Sockets.TcpClient]::new()
        $frontendReady = $client.ConnectAsync("127.0.0.1", 5173).Wait(1000)
        $client.Dispose()
    } catch { }
    if ($backendReady -and $frontendReady) { break }
    Start-Sleep -Seconds 1
}
if (-not $backendReady) { throw "Backend did not become healthy. Check target/demo-backend-error.log." }
if (-not $frontendReady) { throw "Frontend did not become ready. Check target/demo-frontend-error.log." }

Write-Host "Demo ready: http://localhost:5173"
Write-Host "Analyst: analyst / analyst-demo"
Write-Host "Senior analyst: senior / senior-demo"
Write-Host "Stop with: .\scripts\stop-demo.cmd"
