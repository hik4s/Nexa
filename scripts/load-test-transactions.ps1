param(
    [string]$BaseUrl = "http://localhost:8080",
    [int]$SeedTransactions = 500,
    [int]$Requests = 200,
    [int]$Concurrency = 20,
    [int]$PageSize = 100,
    [int]$Offset = 0
)

$ErrorActionPreference = "Stop"
if ($SeedTransactions -lt 1) { throw "SeedTransactions must be >= 1." }
if ($Requests -lt 1) { throw "Requests must be >= 1." }
if ($Concurrency -lt 1) { throw "Concurrency must be >= 1." }
if ($PageSize -lt 1 -or $PageSize -gt 200) { throw "PageSize must be between 1 and 200." }
if ($Offset -lt 0) { throw "Offset must be >= 0." }

$base = $BaseUrl.TrimEnd('/')
$email = "nexa-load-$(New-Guid)@example.test"
$password = "LoadTest-$(New-Guid)!aA1"
$seedPath = Join-Path $env:TEMP "nexa-load-$([Guid]::NewGuid().ToString('N')).sql"
$seedUserId = $null
$accountId = $null

function Invoke-Json {
    param(
        [ValidateSet('GET','POST')][string]$Method,
        [string]$Uri,
        [hashtable]$Headers = @{},
        [string]$Body = $null
    )
    $params = @{
        Method = $Method
        Uri = $Uri
        Headers = $Headers
        TimeoutSec = 30
    }
    if ($null -ne $Body) {
        $params.ContentType = 'application/json'
        $params.Body = $Body
    }
    Invoke-RestMethod @params
}

function Cleanup {
    try {
        $cleanupSql = @"
BEGIN;
CREATE TEMP TABLE load_cleanup_users ON COMMIT DROP AS
    SELECT id FROM users WHERE email = '$email';
CREATE TEMP TABLE load_cleanup_accounts ON COMMIT DROP AS
    SELECT id FROM accounts
    WHERE owner_id IN (SELECT id FROM load_cleanup_users);
DELETE FROM transactions
WHERE account_id IN (SELECT id FROM load_cleanup_accounts);
DELETE FROM sync_changes
WHERE user_id IN (SELECT id FROM load_cleanup_users);
DELETE FROM accounts
WHERE id IN (SELECT id FROM load_cleanup_accounts);
DELETE FROM users
WHERE id IN (SELECT id FROM load_cleanup_users);
COMMIT;
"@
        $cleanupSql | docker exec -i nexa-finance-postgres psql -U nexa -d nexa_finance -v ON_ERROR_STOP=1 | Out-Null
    } catch {
        Write-Warning "Load-test cleanup failed: $($_.Exception.Message)"
    } finally {
        Remove-Item $seedPath -Force -ErrorAction SilentlyContinue
    }
}

try {
    Write-Output "=== Provisioning isolated load-test user ==="
    $registerBody = @{ email = $email; password = $password; displayName = 'NEXA Load Test'; timeZone = 'America/Sao_Paulo' } | ConvertTo-Json
    $register = Invoke-Json -Method POST -Uri "$base/api/v1/auth/register" -Body $registerBody

    $loginBody = @{ email = $email; password = $password } | ConvertTo-Json
    $login = Invoke-Json -Method POST -Uri "$base/api/v1/auth/login" -Body $loginBody
    $seedUserId = $login.userId
    $token = $login.token
    if ([string]::IsNullOrWhiteSpace($token)) { throw "Login did not return a token." }
    $headers = @{ Authorization = "Bearer $token" }

    $accountBody = @{ name = 'Load Test Account'; type = 'CHECKING'; openingBalanceMinor = 0; currency = 'BRL' } | ConvertTo-Json
    $account = Invoke-Json -Method POST -Uri "$base/api/v1/accounts" -Headers $headers -Body $accountBody
    $accountId = $account.id
    if ([string]::IsNullOrWhiteSpace($accountId)) { throw "Account creation did not return an id." }

    Write-Output "=== Seeding $SeedTransactions transactions directly in PostgreSQL ==="
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
    '$accountId'::uuid,
    100 + (g % 50000),
    'BRL',
    'EXPENSE',
    'DEBIT',
    'POSTED',
    CURRENT_DATE - (g % 365),
    'API-LOAD-' || g,
    NULL, NULL, NULL, NULL, NULL,
    NULL, NULL,
    'MANUAL',
    NULL,
    CURRENT_TIMESTAMP - (((g)::text || ' seconds')::interval),
    CURRENT_TIMESTAMP - (((g)::text || ' seconds')::interval)
FROM generate_series(1, $SeedTransactions) AS g;
ANALYZE transactions;
COMMIT;
"@ | Set-Content -Path $seedPath -Encoding UTF8

    Get-Content $seedPath | docker exec -i nexa-finance-postgres psql -U nexa -d nexa_finance -v ON_ERROR_STOP=1 | Out-Null

    $uri = "$base/api/v1/transactions?limit=$PageSize&offset=$Offset"
    $workerCount = [Math]::Min($Concurrency, $Requests)
    $basePerWorker = [Math]::Floor($Requests / $workerCount)
    $remainder = $Requests % $workerCount

    Write-Output "=== Load test: GET /api/v1/transactions ==="
    Write-Output "URL: $uri"
    Write-Output "Seeded rows: $SeedTransactions"
    Write-Output "Requests: $Requests | Concurrency: $Concurrency"

    $workerScript = {
        param($target, $token, $count)
        $results = New-Object System.Collections.Generic.List[object]
        for ($i = 0; $i -lt $count; $i++) {
            $watch = [System.Diagnostics.Stopwatch]::StartNew()
            $ok = $false
            $statusCode = $null
            $errorMessage = $null
            try {
                $headers = @{ Authorization = "Bearer $token" }
                $response = Invoke-WebRequest -UseBasicParsing -Uri $target -Headers $headers -TimeoutSec 30
                $statusCode = [int]$response.StatusCode
                $ok = ($statusCode -eq 200)
            } catch {
                if ($_.Exception.Response -and $_.Exception.Response.StatusCode) {
                    try { $statusCode = [int]$_.Exception.Response.StatusCode } catch {}
                }
                $errorMessage = $_.Exception.Message
                $ok = $false
            } finally {
                $watch.Stop()
            }
            $results.Add([pscustomobject]@{
                Success = $ok
                StatusCode = $statusCode
                Error = $errorMessage
                LatencyMs = $watch.ElapsedMilliseconds
            })
        }
        $results
    }

    $pool = [RunspaceFactory]::CreateRunspacePool(1, $workerCount)
    $pool.Open()
    $workerPipelines = @()
    $workerAsync = @()

    try {
        $overall = [System.Diagnostics.Stopwatch]::StartNew()
        for ($worker = 0; $worker -lt $workerCount; $worker++) {
            $count = $basePerWorker + $(if ($worker -lt $remainder) { 1 } else { 0 })
            $pipeline = [PowerShell]::Create()
            $pipeline.RunspacePool = $pool
            [void]$pipeline.AddScript($workerScript.ToString())
            [void]$pipeline.AddArgument($uri)
            [void]$pipeline.AddArgument($token)
            [void]$pipeline.AddArgument($count)
            $workerPipelines += $pipeline
            $workerAsync += $pipeline.BeginInvoke()
        }

        for ($i = 0; $i -lt $workerAsync.Count; $i++) {
            while (-not $workerAsync[$i].IsCompleted) {
                Start-Sleep -Milliseconds 25
            }
        }

        $results = foreach ($i in 0..($workerPipelines.Count - 1)) {
            $workerPipelines[$i].EndInvoke($workerAsync[$i])
        }
        $overall.Stop()
    } finally {
        foreach ($pipeline in $workerPipelines) {
            $pipeline.Dispose()
        }
        $pool.Close()
        $pool.Dispose()
    }

    $results = @($results | Where-Object { $_ -is [psobject] })
    $ordered = @($results.LatencyMs | Sort-Object)
    if ($ordered.Count -eq 0) { throw "Load test produced no results." }
    $successful = @($results | Where-Object Success)
    $failed = @($results | Where-Object { -not $_.Success })
    $p50 = $ordered[[Math]::Min($ordered.Count - 1, [int][Math]::Floor($ordered.Count * 0.50))]
    $p95 = $ordered[[Math]::Min($ordered.Count - 1, [int][Math]::Floor($ordered.Count * 0.95))]
    $p99 = $ordered[[Math]::Min($ordered.Count - 1, [int][Math]::Floor($ordered.Count * 0.99))]

    $summary = [pscustomobject]@{
        Url = $uri
        SeedTransactions = $SeedTransactions
        Requests = $Requests
        Concurrency = $Concurrency
        Success = $successful.Count
        Failed = $failed.Count
        DurationMs = $overall.ElapsedMilliseconds
        RequestsPerSecond = [Math]::Round($Requests / [Math]::Max(0.001, $overall.Elapsed.TotalSeconds), 2)
        P50Ms = $p50
        P95Ms = $p95
        P99Ms = $p99
    }
    $summary | Format-List

    if ($failed.Count -gt 0) {
        Write-Output "=== Failure breakdown ==="
        $failed | Group-Object StatusCode, Error | Sort-Object Count -Descending |
            Select-Object -First 10 Count, Name | Format-Table -AutoSize
        throw "$($failed.Count) of $Requests requests failed."
    }
    if ($successful.Count -ne $Requests) {
        throw "Expected $Requests successful requests, got $($successful.Count)."
    }
} finally {
    Write-Output "=== Cleaning isolated load-test data ==="
    Cleanup
}
