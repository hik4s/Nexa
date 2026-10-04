param(
    [int]$TransactionsPerAccount = 10000,
    [int]$Accounts = 5,
    [int]$PageSize = 100,
    [int]$Offset = 0
)

$ErrorActionPreference = 'Stop'
if ($TransactionsPerAccount -lt 1) { throw 'TransactionsPerAccount must be >= 1.' }
if ($Accounts -lt 1) { throw 'Accounts must be >= 1.' }
if ($PageSize -lt 1) { throw 'PageSize must be >= 1.' }
if ($Offset -lt 0) { throw 'Offset must be >= 0.' }

$candidateLimit = $PageSize + $Offset

$sql = @'
BEGIN;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM users) THEN
        RAISE EXCEPTION 'Benchmark requires at least one user in the local database.';
    END IF;
END $$;

INSERT INTO accounts (
    id, owner_id, name, type, currency, opening_balance_minor, status, created_at, updated_at
)
SELECT
    ('20000000-0000-4000-8000-' || lpad(g::text, 12, '0'))::uuid,
    (SELECT id FROM users ORDER BY id LIMIT 1),
    'PERF-' || g,
    'CHECKING',
    'BRL',
    0,
    'ACTIVE',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM generate_series(1, __ACCOUNTS__) AS g;

INSERT INTO transactions (
    id, account_id, amount_minor, currency, type, direction, status, occurred_on,
    description, category_id, card_id, invoice_id, installment_id, installment_number,
    recurrence_id, recurrence_occurrence_number, source, related_transaction_id,
    created_at, updated_at
)
SELECT
    gen_random_uuid(),
    ('20000000-0000-4000-8000-' || lpad(a::text, 12, '0'))::uuid,
    100 + (n % 50000),
    'BRL',
    'EXPENSE',
    'DEBIT',
    'POSTED',
    CURRENT_DATE - (n % 365),
    'PERF-' || a || '-' || n,
    NULL, NULL, NULL, NULL, NULL, NULL, NULL,
    'MANUAL',
    NULL,
    CURRENT_TIMESTAMP - (((n)::text || ' seconds')::interval),
    CURRENT_TIMESTAMP - (((n)::text || ' seconds')::interval)
FROM generate_series(1, __ACCOUNTS__) AS a
CROSS JOIN generate_series(1, __TX_PER_ACCOUNT__) AS n;

ANALYZE accounts;
ANALYZE transactions;

EXPLAIN (ANALYZE, BUFFERS, SUMMARY, COSTS OFF)
SELECT t.*
FROM accounts a
CROSS JOIN LATERAL (
    SELECT t.*
    FROM transactions t
    WHERE t.account_id = a.id
    ORDER BY t.occurred_on DESC, t.created_at DESC, t.id DESC
    LIMIT __CANDIDATE_LIMIT__
) t
WHERE a.owner_id = (SELECT id FROM users ORDER BY id LIMIT 1)
ORDER BY t.occurred_on DESC, t.created_at DESC, t.id DESC
LIMIT __PAGE_SIZE__ OFFSET __OFFSET__;

ROLLBACK;
'@

$sql = $sql.Replace('__ACCOUNTS__', [string]$Accounts)
$sql = $sql.Replace('__TX_PER_ACCOUNT__', [string]$TransactionsPerAccount)
$sql = $sql.Replace('__CANDIDATE_LIMIT__', [string]$candidateLimit)
$sql = $sql.Replace('__PAGE_SIZE__', [string]$PageSize)
$sql = $sql.Replace('__OFFSET__', [string]$Offset)

$output = $sql | docker exec -i nexa-finance-postgres psql -U nexa -d nexa_finance -v ON_ERROR_STOP=1
$output | Write-Output
if ($LASTEXITCODE -ne 0) {
    throw "PostgreSQL benchmark failed with exit code $LASTEXITCODE."
}
