# NEXA Finance — Arquitetura inicial

## Monorepo

```text
nexa-finance/
├── android/
│   └── app/
├── backend/
├── docs/
└── infra/
```

## Android

```text
Presentation
  ├── screens
  ├── components
  └── theme
      ↓
ViewModel
      ↓
Use Cases
      ↓
Domain
      ↓
Repositories
      ↓
Data Sources / API
```

## Backend

```text
HTTP / Routing
      ↓
Application / Use Cases
      ↓
Domain
      ↓
Repositories
      ↓
PostgreSQL / External services
```

A implementação por domínio será ampliada conforme cada domínio financeiro entrar no produto.

## Regra fundamental

UI não calcula saldo, fatura, parcelas, estornos ou regras de categorização. Essas responsabilidades pertencem ao domínio/aplicação.
