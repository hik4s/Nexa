param(
    [string]$BaseUrl = "http://localhost:8080",
    [string]$Path = "/health",
    [string]$Token = "",
    [int]$Requests = 100,
    [int]$Concurrency = 10
)

$ErrorActionPreference = "Stop"
if ($Requests -lt 1) { throw "Requests must be >= 1." }
if ($Concurrency -lt 1) { throw "Concurrency must be >= 1." }

$uri = "$($BaseUrl.TrimEnd('/'))/$($Path.TrimStart('/'))"
$workerCount = [Math]::Min($Concurrency, $Requests)
$basePerWorker = [Math]::Floor($Requests / $workerCount)
$remainder = $Requests % $workerCount

$jobs = @()
for ($worker = 0; $worker -lt $workerCount; $worker++) {
    $count = $basePerWorker + $(if ($worker -lt $remainder) { 1 } else { 0 })
    $jobs += Start-Job -ScriptBlock {
        param($target, $token, $count)

        $results = New-Object System.Collections.Generic.List[object]
        for ($i = 0; $i -lt $count; $i++) {
            $watch = [System.Diagnostics.Stopwatch]::StartNew()
            $ok = $false
            try {
                $headers = @{}
                if (-not [string]::IsNullOrWhiteSpace($token)) {
                    $headers["Authorization"] = "Bearer $token"
                }
                $response = Invoke-WebRequest -UseBasicParsing -Uri $target -Headers $headers -TimeoutSec 15
                $ok = ($response.StatusCode -ge 200 -and $response.StatusCode -lt 300)
            } catch {
                $ok = $false
            } finally {
                $watch.Stop()
            }
            $results.Add([pscustomobject]@{
                Success = $ok
                LatencyMs = $watch.ElapsedMilliseconds
            })
        }
        $results
    } -ArgumentList $uri, $Token, $count
}

$overall = [System.Diagnostics.Stopwatch]::StartNew()
Wait-Job -Job $jobs | Out-Null
$results = foreach ($job in $jobs) { Receive-Job -Job $job }
Remove-Job -Job $jobs -Force | Out-Null
$overall.Stop()

$ordered = @($results.LatencyMs | Sort-Object)
$successful = @($results | Where-Object Success)
$failed = @($results | Where-Object { -not $_.Success })
$p50 = $ordered[[Math]::Min($ordered.Count - 1, [int]($ordered.Count * 0.50))]
$p95 = $ordered[[Math]::Min($ordered.Count - 1, [int]($ordered.Count * 0.95))]
$p99 = $ordered[[Math]::Min($ordered.Count - 1, [int]($ordered.Count * 0.99))]

[pscustomobject]@{
    Url = $uri
    Requests = $Requests
    Concurrency = $Concurrency
    Success = $successful.Count
    Failed = $failed.Count
    DurationMs = $overall.ElapsedMilliseconds
    RequestsPerSecond = [Math]::Round($Requests / [Math]::Max(0.001, $overall.Elapsed.TotalSeconds), 2)
    P50Ms = $p50
    P95Ms = $p95
    P99Ms = $p99
} | Format-List
