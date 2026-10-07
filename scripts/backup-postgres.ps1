param(
    [string]$OutputDirectory = "$PSScriptRoot\..\backups"
)

$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
Set-Location $root

if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
    throw "Docker is required."
}

$envFile = Join-Path $root ".env"
if (-not (Test-Path $envFile)) {
    throw ".env is required."
}

function Read-EnvValue([string]$name) {
    $line = Get-Content $envFile | Where-Object { $_ -match "^$name=" } | Select-Object -First 1
    if (-not $line) { return $null }
    return ($line -split "=", 2)[1]
}

$db = Read-EnvValue "POSTGRES_DB"
$user = Read-EnvValue "POSTGRES_USER"
if ([string]::IsNullOrWhiteSpace($db)) { $db = "nexa_finance" }
if ([string]::IsNullOrWhiteSpace($user)) { $user = "nexa" }

New-Item -ItemType Directory -Force -Path $OutputDirectory | Out-Null
$stamp = Get-Date -Format "yyyyMMdd-HHmmss"
$file = Join-Path $OutputDirectory "nexa_finance-$stamp.dump"

docker exec nexa-finance-postgres pg_dump -U $user -Fc -d $db -f "/tmp/$([IO.Path]::GetFileName($file))"
if ($LASTEXITCODE -ne 0) { throw "pg_dump failed." }

docker cp "nexa-finance-postgres:/tmp/$([IO.Path]::GetFileName($file))" $file
if ($LASTEXITCODE -ne 0) { throw "docker cp failed." }

docker exec nexa-finance-postgres rm -f "/tmp/$([IO.Path]::GetFileName($file))"
Write-Host "Backup created: $file" -ForegroundColor Green
