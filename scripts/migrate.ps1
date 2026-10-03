$ErrorActionPreference = 'Stop'
$repoRoot = Split-Path -Parent $PSScriptRoot
$envFile = Join-Path $repoRoot '.env'

if (-not (Test-Path $envFile)) {
    throw '.env was not found. Copy .env.example to .env and set local database credentials.'
}

Get-Content $envFile |
    Where-Object { $_ -match '^\s*[A-Za-z_][A-Za-z0-9_]*=' -and $_ -notmatch '^\s*#' } |
    ForEach-Object {
        $parts = $_ -split '=', 2
        $name = $parts[0].Trim()
        $value = $parts[1].Trim()
        [Environment]::SetEnvironmentVariable($name, $value, 'Process')
    }

Push-Location (Join-Path $repoRoot 'backend')
try {
    .\gradlew.bat :migrateDatabase
    if ($LASTEXITCODE -ne 0) {
        throw "Flyway migration failed with exit code $LASTEXITCODE."
    }
} finally {
    Pop-Location
}
