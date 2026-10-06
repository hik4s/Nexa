#!/bin/sh
set -eu

: "${PGHOST:?PGHOST is required}"
: "${PGPORT:?PGPORT is required}"
: "${PGUSER:?PGUSER is required}"
: "${PGDATABASE:?PGDATABASE is required}"
: "${PGPASSWORD:?PGPASSWORD is required}"
: "${S3_ENDPOINT:?S3_ENDPOINT is required}"
: "${S3_BUCKET:?S3_BUCKET is required}"
: "${AWS_ACCESS_KEY_ID:?AWS_ACCESS_KEY_ID is required}"
: "${AWS_SECRET_ACCESS_KEY:?AWS_SECRET_ACCESS_KEY is required}"
: "${AWS_DEFAULT_REGION:?AWS_DEFAULT_REGION is required}"

timestamp="$(date -u +%Y%m%dT%H%M%SZ)"
filename="nexa_finance_${timestamp}.dump"
local_file="/tmp/${filename}"
object_key="postgres/production/${filename}"

cleanup() {
    rm -f "$local_file"
}
trap cleanup EXIT INT TERM

echo "Starting PostgreSQL backup: ${filename}"

pg_dump     --format=custom     --no-owner     --no-privileges     --file="$local_file"

test -s "$local_file"
pg_restore --list "$local_file" >/dev/null

aws --endpoint-url "$S3_ENDPOINT" s3 cp     "$local_file"     "s3://${S3_BUCKET}/${object_key}"     --only-show-errors

aws --endpoint-url "$S3_ENDPOINT" s3api head-object     --bucket "$S3_BUCKET"     --key "$object_key"     >/dev/null

echo "PostgreSQL backup uploaded and verified: ${object_key}"
