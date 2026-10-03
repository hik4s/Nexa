# NEXA Finance — Local infrastructure

The local infrastructure uses Docker Compose with PostgreSQL 18.

## First setup

From the repository root:

```powershell
Copy-Item .env.example .env
```

Edit `.env` and set `POSTGRES_PASSWORD` to a local-only development password.

Then validate and start the stack:

```powershell
docker compose --env-file .env -f .\infra\docker-compose.yml config
docker compose --env-file .env -f .\infra\docker-compose.yml up -d
docker compose --env-file .env -f .\infra\docker-compose.yml ps
```

The PostgreSQL 18 image uses `/var/lib/postgresql` as the persistent volume mount. Do not change it back to `/var/lib/postgresql/data` for PostgreSQL 18+.

## Resetting the local database

This destroys the local PostgreSQL data volume and is safe only for the disposable development database:

```powershell
docker compose --env-file .env -f .\infra\docker-compose.yml down -v
docker compose --env-file .env -f .\infra\docker-compose.yml up -d
```
