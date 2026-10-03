param(
    [switch]$Fast,
    [switch]$SkipAndroid,
    [switch]$SkipInfra
)

$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
Set-Location $root

function Step($name, [scriptblock]$action) {
    Write-Host "`n=== $name ===" -ForegroundColor Cyan
    & $action
    if ($LASTEXITCODE -ne 0) { throw "Falha em: $name (exit code $LASTEXITCODE)" }
}

Write-Host "NEXA Finance verification" -ForegroundColor Green
Write-Host "Root: $root"

if (-not (Test-Path "$root\.env")) {
    Write-Warning ".env não encontrado. Isso só é obrigatório para a validação da infraestrutura."
}

Step 'Java 17' {
    java -version
}

Step 'Backend tests' {
    Push-Location "$root\backend"
    try {
        .\gradlew.bat clean test
    } finally { Pop-Location }
}

Step 'Backend build' {
    Push-Location "$root\backend"
    try {
        .\gradlew.bat build
    } finally { Pop-Location }
}

if (-not $Fast -and -not $SkipAndroid) {
    Step 'Android build' {
        Push-Location "$root\android"
        try {
            .\gradlew.bat clean test :app:assembleDebug
        } finally { Pop-Location }
    }
}

if (-not $Fast -and -not $SkipInfra) {
    if (Get-Command docker -ErrorAction SilentlyContinue) {
        Step 'Docker Compose config' {
            docker compose --env-file "$root\.env" -f "$root\infra\docker-compose.yml" config | Out-Null
        }
    } else {
        Write-Warning 'Docker não está disponível; validação de infraestrutura ignorada.'
    }
}

Write-Host "`nNEXA verification: PASS" -ForegroundColor Green
