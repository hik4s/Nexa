param(
    [int]$SeedTransactionsPerAccount = 10000,
    [int]$Accounts = 5,
    [int]$Clients = 25,
    [int]$Threads = 5,
    [int]$DurationSeconds = 15
)

$ErrorActionPreference = 'Stop'
if ($SeedTransactionsPerAccount -lt 100) { throw 'SeedTransactionsPerAccount must be >= 100.' }
if ($Accounts -lt 1) { throw 'Accounts must be >= 1.' }
if ($Clients -lt 1) { throw 'Clients must be >= 1.' }
if ($Threads -lt 1 -or $Threads -gt $Clients) { throw 'Threads must be between 1 and Clients.' }
if ($DurationSeconds -lt 5) { throw 'DurationSeconds must be >= 5.' }

$repoRoot = Split-Path -Parent $PSScriptRoot
$envFile = Join-Path $repoRoot '.env'
if (-not (Test-Path $envFile)) { throw '.env was not found.' }

$vars = @{}
Get-Content $envFile |
    Where-Object { $_ -match '^\s*[A-Za-z_][A-Za-z0-9_]*=' -and $_ -notmatch '^\s*#' } |
    ForEach-Object {
        $parts = $_ -split '=', 2
        $vars[$parts[0].Trim()] = $parts[1].Trim()
    }

$container = 'nexa-finance-postgres'
$dbName = if ($vars.ContainsKey('POSTGRES_DB') -and $vars.POSTGRES_DB) { $vars.POSTGRES_DB } else { 'nexa_finance' }
$dbUser = if ($vars.ContainsKey('POSTGRES_USER') -and $vars.POSTGRES_USER) { $vars.POSTGRES_USER } else { 'nexa' }
$tag = "NEXA-DB-BENCH-$([Guid]::NewGuid().ToString('N').Substring(0, 12))"
$email = "nexa-db-bench-$([Guid]::NewGuid())@example.test"
$ownerId = [Guid]::NewGuid()
$sqlPath = Join-Path $env:TEMP "nexa-db-benchmark-$([Guid]::NewGuid().ToString('N')).sql"
$seedPath = Join-Path $env:TEMP "nexa-db-seed-$([Guid]::NewGuid().ToString('N')).sql"

function Invoke-Psql {
    param([string]$Sql)
    $output = $Sql | docker exec -i $container psql -U $dbUser -d $dbName -v ON_ERROR_STOP=1
    if ($LASTEXITCODE -ne 0) { throw 'PostgreSQL command failed.' }
    return $output
}

function Cleanup {
    try {
        $cleanup = @"
BEGIN;
DELETE FROM transactions
WHERE account_id IN (SELECT id FROM accounts WHERE owner_id = '$ownerId'::uuid);
DELETE FROM sync_changes WHERE user_id = '$ownerId'::uuid;
DELETE FROM accounts WHERE owner_id = '$ownerId'::uuid;
DELETE FROM users WHERE id = '$ownerId'::uuid;
COMMIT;
"@
        $cleanup | docker exec -i $container psql -U $dbUser -d $dbName -v ON_ERROR_STOP=1 | Out-Null
    } catch {
        Write-Warning "Database benchmark cleanup failed: $($_.Exception.Message)"
    } finally {
        Remove-Item $sqlPath, $seedPath -Force -ErrorAction SilentlyContinue
    }
}try {
    Write-Output '=== PostgreSQL database benchmark ==='
    Write-Output "Database: $dbName"
    Write-Output "Accounts: $Accounts"
    Write-Output "Rows/account: $SeedTransactionsPerAccount"
    Write-Output "Target rows: $($Accounts * $SeedTransactionsPerAccount)"
    Write-Output "Clients: $Clients | Threads: $Threads | Duration: $($DurationSeconds)s"

    $setup = @"
BEGIN;
INSERT INTO users (
    id, email, display_name, time_zone, status, created_at, updated_at
) VALUES (
    '$ownerId'::uuid,
    '$email',
    'NEXA DB Benchmark',
    'America/Sao_Paulo',
    'ACTIVE',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

INSERT INTO accounts (
    id, owner_id, name, type, currency, opening_balance_minor, status, created_at, updated_at
)
SELECT
    gen_random_uuid(),
    '$ownerId'::uuid,
    '$tag-' || g,
    'CHECKING',
    'BRL',
    0,
    'ACTIVE',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM generate_series(1, $Accounts) AS g;
COMMIT;
"@
    Invoke-Psql $setup | Out-Null

    Write-Output 'Seeding benchmark transactions...'
    @"
BEGIN;
INSERT INTO transactions (
    id, account_id, amount_minor, currency, type, direction, status, occurred_on,
    description, category_id, card_id, invoice_id, installment_id, installment_number,
    recurrence_id, recurrence_occurrence_number, source, related_transaction_id,
    created_at, updated_at
)
SELECT
    gen_random_uuid(),
    a.id,
    100 + (g % 50000),
    'BRL',
    'EXPENSE',
    'DEBIT',
    'POSTED',
    CURRENT_DATE - (g % 365),
    'DB-BENCH-' || g,
    NULL, NULL, NULL, NULL, NULL,
    NULL, NULL,
    'MANUAL',
    NULL,
    CURRENT_TIMESTAMP - (((g)::text || ' seconds')::interval),
    CURRENT_TIMESTAMP - (((g)::text || ' seconds')::interval)
FROM accounts a
CROSS JOIN generate_series(1, $SeedTransactionsPerAccount) AS g
WHERE a.owner_id = '$ownerId'::uuid
  AND a.name LIKE '$tag-%';
ANALYZE transactions;
COMMIT;
"@ | Set-Content -Path $seedPath -Encoding UTF8

    Get-Content $seedPath | docker exec -i $container psql -U $dbUser -d $dbName -v ON_ERROR_STOP=1 | Out-Null
    if ($LASTEXITCODE -ne 0) { throw 'Database seeding failed.' }

    $counts = docker exec $container psql -U $dbUser -d $dbName -Atc "SELECT count(*) FROM transactions t JOIN accounts a ON a.id=t.account_id WHERE a.owner_id='$ownerId'::uuid;"
    if ($LASTEXITCODE -ne 0) { throw 'Could not verify seeded rows.' }
    Write-Output "Seeded rows: $counts"
    if ([int]$counts -ne ($Accounts * $SeedTransactionsPerAccount)) {
        throw "Expected $($Accounts * $SeedTransactionsPerAccount) seeded rows, got $counts."
    }

    $benchmarkSql = @"
SELECT t.id, t.account_id, t.amount_minor, t.currency, t.type, t.direction,
       t.status, t.occurred_on, t.description, t.category_id, t.card_id,
       t.invoice_id, t.installment_id, t.installment_number, t.recurrence_id,
       t.recurrence_occurrence_number, t.source, t.related_transaction_id,
       t.created_at, t.updated_at, t.version
FROM accounts a
CROSS JOIN LATERAL (
    SELECT t.*
    FROM transactions t
    WHERE t.account_id = a.id
    ORDER BY t.occurred_on DESC, t.created_at DESC, t.id DESC
    LIMIT 100
) t
WHERE a.owner_id = '$ownerId'::uuid
ORDER BY t.occurred_on DESC, t.created_at DESC, t.id DESC
LIMIT 100 OFFSET 0;
"@
    $benchmarkSql | docker exec -i $container sh -lc "cat > /tmp/nexa-db-benchmark.sql"

    Write-Output 'Running pgbench...'
    $pgbenchStdoutPath = Join-Path $env:TEMP "nexa-pgbench-out-$([Guid]::NewGuid().ToString('N')).log"
    $pgbenchStderrPath = Join-Path $env:TEMP "nexa-pgbench-err-$([Guid]::NewGuid().ToString('N')).log"
    try {
        $pgbenchArgs = @(
            'exec', $container, 'pgbench', '-U', $dbUser, '-d', $dbName,
            '-n', '-M', 'prepared', '-c', $Clients, '-j', $Threads,
            '-T', $DurationSeconds, '-P', '5', '-f', '/tmp/nexa-db-benchmark.sql'
        )
        $pgbenchProcess = Start-Process -FilePath 'docker.exe' -ArgumentList ($pgbenchArgs -join ' ') -NoNewWindow -Wait -PassThru -RedirectStandardOutput $pgbenchStdoutPath -RedirectStandardError $pgbenchStderrPath
        $pgbenchOutput = @(Get-Content $pgbenchStdoutPath -ErrorAction SilentlyContinue)
        $pgbenchErrors = @(Get-Content $pgbenchStderrPath -ErrorAction SilentlyContinue)
        $pgbenchOutput | ForEach-Object { Write-Output $_ }
        $pgbenchErrors | ForEach-Object { Write-Output $_ }
        if ($pgbenchProcess.ExitCode -ne 0) { throw "pgbench failed with exit code $($pgbenchProcess.ExitCode)." }
    } finally {
        Remove-Item $pgbenchStdoutPath, $pgbenchStderrPath -Force -ErrorAction SilentlyContinue
    }

    Write-Output '=== Benchmark completed successfully ==='
} finally {
    Write-Output '=== Cleaning isolated database benchmark data ==='
    Cleanup
}