$ErrorActionPreference = 'Stop'
$repoRoot = Split-Path -Parent $PSScriptRoot
$envFile = Join-Path $repoRoot '.env'

if (-not (Test-Path $envFile)) {
    throw '.env was not found.'
}

$vars = @{}
Get-Content $envFile |
    Where-Object { $_ -match '^\s*[A-Za-z_][A-Za-z0-9_]*=' -and $_ -notmatch '^\s*#' } |
    ForEach-Object {
        $parts = $_ -split '=', 2
        $vars[$parts[0].Trim()] = $parts[1].Trim()
    }

$dbName = if ($vars.ContainsKey('POSTGRES_DB') -and $vars.POSTGRES_DB) { $vars.POSTGRES_DB } else { 'nexa_finance' }
$dbUser = if ($vars.ContainsKey('POSTGRES_USER') -and $vars.POSTGRES_USER) { $vars.POSTGRES_USER } else { 'nexa' }
$container = 'nexa-finance-postgres'

$expectedTables = @(
    'accounts',
    'categories',
    'installment_items',
    'installments',
    'recurrences',
    'transactions',
    'transfers'
)

$tableQuery = "SELECT string_agg(table_name, ',' ORDER BY table_name) FROM information_schema.tables WHERE table_schema='public' AND table_name NOT LIKE 'flyway%';"
$tables = docker exec $container psql -U $dbUser -d $dbName -Atc $tableQuery
if ($LASTEXITCODE -ne 0) { throw 'Could not query PostgreSQL tables.' }
$actualTables = if ($tables) { $tables -split ',' } else { @() }

$actualNormalized = (@($actualTables | Sort-Object) -join ',')
$expectedNormalized = (@($expectedTables | Sort-Object) -join ',')
if ($actualNormalized -ne $expectedNormalized) {
    throw "Unexpected table set: $tables"
}

$migrationQuery = "SELECT version || '|' || success FROM flyway_schema_history ORDER BY installed_rank;"
$migration = docker exec $container psql -U $dbUser -d $dbName -Atc $migrationQuery
if ($LASTEXITCODE -ne 0) { throw 'Could not query Flyway schema history.' }
$actualMigrations = @($migration -split '\r?\n' | Where-Object { $_.Trim() -ne '' } | ForEach-Object { $_.Trim() })
$expectedMigrations = @('1|true', '2|true')
if (($actualMigrations -join '|') -ne ($expectedMigrations -join '|')) { throw "Unexpected Flyway history: $migration" }

$indexQuery = "SELECT count(*) FROM pg_indexes WHERE schemaname='public' AND indexname LIKE 'idx_%';"
$indexCount = [int](docker exec $container psql -U $dbUser -d $dbName -Atc $indexQuery)
if ($indexCount -lt 13) { throw "Expected at least 13 application indexes, found $indexCount." }

$fkQuery = "SELECT count(*) FROM information_schema.table_constraints WHERE constraint_schema='public' AND constraint_type='FOREIGN KEY' AND table_name <> 'flyway_schema_history';"
$foreignKeyCount = [int](docker exec $container psql -U $dbUser -d $dbName -Atc $fkQuery)
if ($foreignKeyCount -lt 12) { throw "Expected at least 12 foreign keys, found $foreignKeyCount." }

Write-Host "Database verification: PASS"
Write-Host "Tables: $($expectedTables -join ', ')"
Write-Host "Migration: $migration"
Write-Host "Indexes: $indexCount"
Write-Host "Foreign keys: $foreignKeyCount"
