# NEXA PostgreSQL production backup

The backup worker runs `pg_dump` against the Railway PostgreSQL service, validates the custom-format archive with `pg_restore --list`, uploads it to the Railway S3-compatible object bucket, and verifies the uploaded object with `head-object`.

The worker does not log database credentials or storage secrets.

Production configuration is supplied through Railway environment-variable references:
- PGHOST
- PGPORT
- PGUSER
- PGPASSWORD
- PGDATABASE
- S3_ENDPOINT
- S3_BUCKET
- AWS_ACCESS_KEY_ID
- AWS_SECRET_ACCESS_KEY
- AWS_DEFAULT_REGION

The scheduled worker is intentionally separate from the API service so backup failures cannot take down request handling.
