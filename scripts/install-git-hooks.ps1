$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
Set-Location $root

git config core.hooksPath .githooks
Write-Host 'Git hooks configurados em .githooks' -ForegroundColor Green
