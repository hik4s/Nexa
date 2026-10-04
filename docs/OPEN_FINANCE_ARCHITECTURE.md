# NEXA Finance — Open Finance Architecture

Status: architecture prepared; live provider connection remains external.

## Boundary

The NEXA domain must not depend directly on a single Open Finance provider.

Recommended flow:

User consent
→ Provider Adapter
→ Provider API
→ Normalization
→ Reconciliation
→ NEXA ledger

## Provider adapter

Define an interface with:

- create consent/session
- get consent status
- list accounts
- list transactions
- list cards when supported
- revoke consent
- map provider errors
- expose provider request identifiers for audit

The adapter returns provider-neutral records. The financial domain never stores provider-specific payloads as its canonical representation.

## Consent

Consent records should contain:

- user
- provider
- external consent identifier
- scopes
- created time
- expiration/revocation time
- current state
- audit timestamps

Access tokens and refresh tokens must never be stored in plaintext application tables.

## Reconciliation

Imported transactions need stable external identity:

provider + institution + account + external transaction id

Fallback matching should use conservative fingerprints based on date, amount, direction and normalized description. A low-confidence match must never silently merge financial events.

## Duplicate policy

- exact external identity: idempotent update
- strong fingerprint: candidate for reconciliation
- ambiguous match: manual review
- conflicting financial amount: never overwrite the canonical ledger silently

## Monitoring

Every provider synchronization should expose:

- last successful sync
- last attempted sync
- latency
- item count
- new/imported/updated/duplicate/conflict counts
- provider error category
- retry count

## External prerequisites

A real connection requires a selected provider/participant, onboarding, credentials, consent flow, sandbox or production access, security review and operational monitoring. Those cannot be marked complete from the local repository alone.
