[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)]
    [string]$Source,

    [string]$Output
)

$ErrorActionPreference = "Stop"
$toolingRoot = Split-Path -Parent $PSScriptRoot

$sourcePath = if ([System.IO.Path]::IsPathRooted($Source)) {
    $Source
} else {
    Join-Path (Get-Location) $Source
}
$sourcePath = [System.IO.Path]::GetFullPath($sourcePath)

if (-not (Test-Path -LiteralPath $sourcePath -PathType Leaf)) {
    throw "Mermaid source file not found: $sourcePath"
}

if ([string]::IsNullOrWhiteSpace($Output)) {
    $outputPath = [System.IO.Path]::ChangeExtension($sourcePath, ".svg")
} elseif ([System.IO.Path]::IsPathRooted($Output)) {
    $outputPath = $Output
} else {
    $outputPath = Join-Path (Get-Location) $Output
}
$outputPath = [System.IO.Path]::GetFullPath($outputPath)

$outputDirectory = Split-Path -Parent $outputPath
if (-not (Test-Path -LiteralPath $outputDirectory)) {
    New-Item -ItemType Directory -Path $outputDirectory | Out-Null
}

$configPath = Join-Path $toolingRoot "mermaid.config.json"
& npm exec --prefix $toolingRoot -- mmdc -i $sourcePath -o $outputPath -c $configPath -b transparent
if ($LASTEXITCODE -ne 0) {
    throw "Mermaid rendering failed with exit code $LASTEXITCODE"
}

Write-Output "Rendered Mermaid diagram: $outputPath"
