$projectDir = Split-Path -Parent $PSScriptRoot
$pidFile = Join-Path $projectDir "target\demo-processes.json"
if (-not (Test-Path -LiteralPath $pidFile)) {
    Write-Host "No recorded demo processes."
    exit 0
}
$processes = Get-Content -Raw -LiteralPath $pidFile | ConvertFrom-Json
foreach ($processId in @($processes.backend, $processes.frontend)) {
    & taskkill.exe /PID $processId /T /F 2>$null | Out-Null
}
Remove-Item -LiteralPath $pidFile -ErrorAction SilentlyContinue
Write-Host "Demo stopped."
