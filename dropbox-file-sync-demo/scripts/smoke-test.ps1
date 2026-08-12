$ErrorActionPreference = 'Stop'
$project = Split-Path -Parent $PSScriptRoot
$tempData = Join-Path ([System.IO.Path]::GetTempPath()) ("dropbox-sync-smoke-" + [guid]::NewGuid())
New-Item -ItemType Directory -Path $tempData | Out-Null

Push-Location $project
try {
    & mvn package -q -DskipTests
    if ($LASTEXITCODE -ne 0) { throw 'Maven build failed' }
    $env:SYNC_DATA_DIR = $tempData
    $process = Start-Process -FilePath 'java' -ArgumentList '-jar','target/dropbox-sync-demo-1.0.0.jar','--server.port=8765' -WorkingDirectory $project -PassThru -WindowStyle Hidden
    try {
        $base = 'http://127.0.0.1:8765'
        for ($attempt = 0; $attempt -lt 80; $attempt++) {
            try { Invoke-RestMethod "$base/api/stats" | Out-Null; break } catch { Start-Sleep -Milliseconds 250 }
        }
        if ($attempt -eq 80) { throw 'Server did not start' }

        $bytes = [Text.Encoding]::UTF8.GetBytes('real HTTP file bytes')
        $hasher = [Security.Cryptography.SHA256]::Create()
        try { $sha = ([BitConverter]::ToString($hasher.ComputeHash($bytes))).Replace('-', '').ToLowerInvariant() } finally { $hasher.Dispose() }
        $chunk = @{ hash = $sha; size = $bytes.Length }
        $plan = Invoke-RestMethod "$base/api/uploads/plan" -Method Post -ContentType 'application/json' -Body (@{ chunks = @($chunk) } | ConvertTo-Json -Depth 4)
        if ($plan.missingChunks.Count -ne 1) { throw 'Expected one missing chunk' }
        Invoke-RestMethod "$base/api/chunks/$sha" -Method Put -ContentType 'application/octet-stream' -Body $bytes | Out-Null
        $commitBody = @{ name='smoke.txt'; baseVersion=0; fileHash=$sha; chunks=@($chunk); deviceId='smoke-device' } | ConvertTo-Json -Depth 4
        $created = Invoke-RestMethod "$base/api/commits" -Method Post -ContentType 'application/json' -Headers @{ 'Idempotency-Key'='smoke-create' } -Body $commitBody
        $client = New-Object Net.WebClient
        try { $download = $client.DownloadData("$base/api/files/$($created.fileId)/download") } finally { $client.Dispose() }
        if ([Text.Encoding]::UTF8.GetString($download) -ne 'real HTTP file bytes') { throw 'Downloaded bytes differ' }
        $changes = Invoke-RestMethod "$base/api/changes?cursor=0"
        if ($changes.events.Count -ne 1 -or $changes.events[0].type -ne 'CREATE') { throw 'Change log mismatch' }
        Write-Host 'PASS: real HTTP plan, chunk upload, metadata commit, download, and cursor log'
    } finally {
        if ($process -and -not $process.HasExited) { Stop-Process -Id $process.Id }
    }
} finally {
    Pop-Location
    if (Test-Path -LiteralPath $tempData) { Remove-Item -LiteralPath $tempData -Recurse -Force }
}
