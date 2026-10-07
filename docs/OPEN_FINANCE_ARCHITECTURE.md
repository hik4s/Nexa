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


## 3.3.1 — Regulatory discovery — 2026-10-05

Initial scope is Open Finance data aggregation only. NEXA will not act as a regulated Open Finance participant or payment initiator in this phase.

Regulatory points that shape the product boundary:

- Open Finance participants are institutions covered by the Banco Central framework; the current consolidated rule includes mandatory and voluntary participation categories. NEXA's current product architecture therefore treats the direct-regulated participant relationship as an external dependency unless the company itself later becomes an eligible/authorized participant.
- A customer data-sharing journey includes consent, authentication and confirmation, and those stages must be secure, electronic, successive and uninterrupted. The product must never collect or store the customer's banking password as part of the connection journey.
- Consent remains customer-controlled: the customer must be able to revoke it, and the 2023 rule change allows longer consent validity periods than the former 12-month limit while simplifying renewal. The consent model must support explicit scope, status, creation, expiry/renewal and revocation rather than treating consent as a permanent credential.
- Security, cyber-risk management, LGPD and banking secrecy obligations are part of the regulated ecosystem. The NEXA design must minimize stored financial data, separate provider credentials from the canonical ledger and keep an auditable consent lifecycle.
- The regulatory data scope is broader than transactions alone. NEXA will initially normalize only the resources required for accounts, balances and transactions, then add cards and investments behind separate capability flags.

This is an engineering discovery, not legal advice. Before production onboarding, the selected provider and legal/compliance owner must validate the exact participant model, contracts, data scopes, retention, incident handling and customer disclosures.

## 3.3.2 — Provider/participant mapping — 2026-10-05

Shortlist for technical due diligence, not final selection:

| Candidate | Current evidence | NEXA relevance |
|---|---|---|
| Belvo | Brazil Open Finance aggregation with Hosted Widget, consent management, webhooks and sandbox; documented account/transaction coverage. | Strong fit for a first aggregation adapter and sandbox-first development. |
| Celcoin | Open Finance APIs, sandbox credentials, production mTLS and participant-oriented infrastructure; data-sharing product supports consent and webhooks. | Strong candidate when production regulatory/security requirements need a deeper participant-facing integration. |
| Pluggy | Current 2026 product updates show active Open Finance coverage, but also deprecation of direct PF connectors. | Keep on the shortlist, but verify the target connector strategy before choosing it. |

The NEXA architecture remains provider-neutral. No provider-specific SDK, credential, consent identifier or response schema enters the financial domain.

Primary integration strategy:
NEXA Android -> NEXA backend -> Provider Adapter -> Open Finance participant/provider -> source institution

The backend owns normalization, reconciliation and consent metadata. Provider secrets remain server-side. Webhooks are treated as notifications, not as the canonical ledger source.

### References

- Banco Central — Resolução Conjunta nº 1, consolidated: https://normativos.bcb.gov.br/Lists/Normativos/Attachments/51028/Res_Conj_0001_v8_L.pdf
- Banco Central — Resolução Conjunta nº 7/2023: https://www.bcb.gov.br/estabilidadefinanceira/exibenormativo?numero=7&tipo=Resolução%20Conjunta
- Open Finance Brasil — Participantes: https://openfinancebrasil.org.br/quem-participa/
- Belvo — Aggregation Brazil: https://developers.belvo.com/pt-br/products/aggregation_brazil/aggregation-brazil-introduction
- Celcoin — Open Finance credentials: https://developers.celcoin.com.br/docs/obtendo-acesso-%C3%A0s-apis
- Pluggy — July 2026 product updates: https://docs.pluggy.ai/pt/changelog/2026.07