# NEXA Finance — Sync Protocol

Status: transaction synchronization with optimistic concurrency control.

## Push

`POST /api/v1/sync/push`

Each transaction payload may contain:

- `id`: stable transaction identity.
- `baseVersion`: version last observed by the device when editing an existing transaction.
- transaction fields.

### Create

A new transaction is sent without `baseVersion`.

The server creates it at `version = 1`.

### Update

An existing transaction must provide `baseVersion`.

The server applies the update only when the current database version equals `baseVersion`. A successful update increments the version atomically.

Example:

`version 1 -> update(baseVersion=1) -> version 2`

A stale update is acknowledged as `CONFLICT` by the sync protocol and as HTTP `409 CONFLICT` through the direct update endpoint.

## Retry semantics

A lost ACK must not create a second financial mutation.

When the current transaction already matches the submitted payload and its version is either the submitted `baseVersion` or the immediately following version, the server treats the operation as an idempotent replay.

## Pull

`GET /api/v1/sync/pull?cursor=&limit=`

Pulled transactions include their current `version`.

The Android Room cache stores that version alongside the local transaction.

## Conflict model

The protocol intentionally avoids last-write-wins for financial transactions.

- same ID + same content: idempotent acknowledgement
- same ID + stale `baseVersion`: `CONFLICT`
- same ID + current `baseVersion`: update and increment version
- concurrent writes never silently overwrite a newer server revision

## Current scope

This versioning model is implemented for the transaction aggregate. Other aggregates still need equivalent revision strategies before they are synchronized across devices.