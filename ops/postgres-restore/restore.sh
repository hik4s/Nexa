#!/bin/sh
set -eu

: "${DR_ENV:?DR_ENV is required}"
: "${BACKUP_KEY:?BACKUP_KEY is required}"
: "${S3_ENDPOINT:?S3_ENDPOINT is required}"
: "${S3_BUCKET:?S3_BUCKET is required}"
: "${AWS_ACCESS_KEY_ID:?AWS_ACCESS_KEY_ID is required}"
: "${AWS_SECRET_ACCESS_KEY:?AWS_SECRET_ACCESS_KEY is required}"
: "${AWS_DEFAULT_REGION:?AWS_DEFAULT_REGION is required}"
: "${PGHOST:?PGHOST is required}"
: "${PGPORT:?PGPORT is required}"
: "${PGUSER:?PGUSER is required}"
: "${PGDATABASE:?PGDATABASE is required}"
: "${PGPASSWORD:?PGPASSWORD is required}"

if [ "$DR_ENV" != "isolated" ]; then
    echo "Refusing restore: DR_ENV must be exactly 'isolated'."
    exit 2
fi

WORKFLOW_START=$(date -u +%s)
BACKUP_FILE="/tmp/nexa-restore.dump"
TOC_FILE="/tmp/nexa-restore.toc"

echo "DRILL_START=$(date -u +%Y-%m-%dT%H:%M:%SZ)"
echo "BACKUP_KEY=$BACKUP_KEY"

DOWNLOAD_START=$(date -u +%s)
rm -f "$BACKUP_FILE"
aws --endpoint-url "$S3_ENDPOINT" s3 cp "s3://$S3_BUCKET/$BACKUP_KEY" "$BACKUP_FILE" --only-show-errors
test -s "$BACKUP_FILE"
DOWNLOAD_END=$(date -u +%s)
echo "BACKUP_SIZE_BYTES=$(wc -c < "$BACKUP_FILE" | tr -d ' ')"
echo "BACKUP_DOWNLOAD_SECONDS=$((DOWNLOAD_END-DOWNLOAD_START))"

pg_restore --list "$BACKUP_FILE" > "$TOC_FILE"
echo "TOC_ENTRIES=$(grep -c '^' "$TOC_FILE")"

READY=0
for i in $(seq 1 30); do
    if pg_isready -h "$PGHOST" -p "$PGPORT" -U "$PGUSER" -d "$PGDATABASE" >/dev/null 2>&1; then
        READY=1
        break
    fi
    sleep 2
done
test "$READY" -eq 1

RESTORE_START=$(date -u +%s)
pg_restore --exit-on-error --no-owner --no-privileges -h "$PGHOST" -p "$PGPORT" -U "$PGUSER" -d "$PGDATABASE" "$BACKUP_FILE"
RESTORE_END=$(date -u +%s)
echo "PG_RESTORE_SECONDS=$((RESTORE_END-RESTORE_START))"

TABLE_COUNT=$(psql -Atqc "SELECT count(*) FROM information_schema.tables WHERE table_schema='public' AND table_type='BASE TABLE'")
INDEX_COUNT=$(psql -Atqc "SELECT count(*) FROM pg_class c JOIN pg_namespace n ON n.oid=c.relnamespace WHERE n.nspname='public' AND c.relkind='i'")
UNVALIDATED_CONSTRAINTS=$(psql -Atqc "SELECT count(*) FROM pg_constraint WHERE NOT convalidated")
INVALID_INDEXES=$(psql -Atqc "SELECT count(*) FROM pg_index WHERE NOT indisvalid OR NOT indisready")
FLYWAY_EXISTS=$(psql -Atqc "SELECT CASE WHEN to_regclass('public.flyway_schema_history') IS NULL THEN 0 ELSE 1 END")
FLYWAY_SUCCESS=$(psql -Atqc "SELECT CASE WHEN to_regclass('public.flyway_schema_history') IS NULL THEN 0 ELSE count(*) FILTER (WHERE success) END FROM public.flyway_schema_history")
FLYWAY_FAILED=$(psql -Atqc "SELECT CASE WHEN to_regclass('public.flyway_schema_history') IS NULL THEN 0 ELSE count(*) FILTER (WHERE NOT success) END FROM public.flyway_schema_history")
LATEST_MIGRATION=$(psql -Atqc "SELECT COALESCE((SELECT version FROM public.flyway_schema_history WHERE success AND version IS NOT NULL ORDER BY installed_rank DESC LIMIT 1),'NONE')")

echo "TABLE_COUNT=$TABLE_COUNT"
echo "INDEX_COUNT=$INDEX_COUNT"
echo "UNVALIDATED_CONSTRAINTS=$UNVALIDATED_CONSTRAINTS"
echo "INVALID_INDEXES=$INVALID_INDEXES"
echo "FLYWAY_EXISTS=$FLYWAY_EXISTS"
echo "FLYWAY_SUCCESS=$FLYWAY_SUCCESS"
echo "FLYWAY_FAILED=$FLYWAY_FAILED"
echo "LATEST_MIGRATION=$LATEST_MIGRATION"

for table in users accounts transactions audit_events sync_changes; do
    exists=$(psql -Atqc "SELECT CASE WHEN to_regclass('public.$table') IS NULL THEN 0 ELSE 1 END")
    if [ "$exists" = "1" ]; then
        count=$(psql -Atqc "SELECT count(*) FROM public.$table")
        echo "TABLE_ROWS_$table=$count"
    fi
done

WORKFLOW_END=$(date -u +%s)
echo "RECOVERY_WORKFLOW_SECONDS=$((WORKFLOW_END-WORKFLOW_START))"
echo "DRILL_END=$(date -u +%Y-%m-%dT%H:%M:%SZ)"
echo "DRILL_STATUS=SUCCESS"
