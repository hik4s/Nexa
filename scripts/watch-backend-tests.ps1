$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
Set-Location "$root\backend"

Write-Host 'NEXA Backend test watcher' -ForegroundColor Green
Write-Host 'Pressione Ctrl+C para sair.'

.\gradlew.bat test --continuous
