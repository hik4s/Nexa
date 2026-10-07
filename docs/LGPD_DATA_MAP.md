# NEXA Finance — LGPD Data Map

Status: technical mapping prepared; legal validation remains external.

## Data categories

### Identity

- email
- display name
- time zone
- authentication/session metadata

### Financial

- account identifiers, type, currency and balances
- transactions, amounts, dates, descriptions and categories
- transfers
- cards and invoice metadata
- installments
- budgets
- goals and contributions
- recurring transactions
- notifications and financial insights

### Operational

- audit/synchronization metadata
- provider identifiers when Open Finance is enabled
- technical logs and request metadata

## Principles

The system must collect only data necessary for the stated product purpose, restrict access by authenticated user, minimize sensitive logging, and avoid placing financial payloads in request logs.

## Technical rights surface

The product should expose:

- profile/data access
- correction
- export in a portable format
- account/data deletion workflow subject to lawful retention requirements
- consent management where applicable

Deletion must be implemented as a policy-driven workflow. It must not blindly erase records that are legally required to be retained.

## Retention

Retention periods must be defined per data class with legal/operational review before production.

## IA exposure

Financial data sent to an AI provider must be minimized, scoped to the requested analysis, and traceable to source facts. Provider-specific retention and training policies must be reviewed before enabling an external model in production.

## External prerequisite

This document is not legal advice. Final privacy policy, terms, lawful bases, retention rules and data-subject procedures require formal review before launch.
