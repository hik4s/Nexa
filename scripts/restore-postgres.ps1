param(
    [Parameter(Mandatory = $true)]
    [string]$BackupFile
)

$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
Set-Location $root

if (-not (Test-Path $BackupFile)) {
    throw "Backup file not found: $BackupFile"
}
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

$remote = "/tmp/nexa-restore-$([IO.Path]::GetFileName($BackupFile))"
docker cp (Resolve-Path $BackupFile).Path "nexa-finance-postgres:$remote"
if ($LASTEXITCODE -ne 0) { throw "docker cp failed." }

docker exec nexa-finance-postgres pg_restore -U $user -d $db --clean --if-exists --no-owner --no-privileges $remote
$restoreExit = $LASTEXITCODE

docker exec nexa-finance-postgres rm -f $remote | Out-Null
if ($restoreExit -ne 0) {
    throw "pg_restore failed with exit code $restoreExit."
}

Write-Host "Restore completed from: $BackupFile" -ForegroundColor Green
