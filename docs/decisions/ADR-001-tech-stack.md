# ADR-001 — Stack tecnológica do NEXA Finance

**Status:** Accepted  
**Data:** 2026-10-03  
**Escopo:** Parte 1.1 — Fechamento técnico

## Decisão

O NEXA Finance será um monorepo com dois aplicativos Gradle independentes:

1. `android/` para o cliente Android.
2. `backend/` para a API REST.

A separação reduz acoplamento de build e permite evoluir o cliente e o servidor de forma independente.

### Android

- Kotlin 2.4.20
- Jetpack Compose
- Material 3 com Design System próprio
- MVVM + princípios de Clean Architecture
- Coroutines + Flow
- Navigation Compose
- Testes unitários e instrumentados
- AGP 9.3.1
- Gradle 9.5.0
- Java toolchain 17

### Backend

- Kotlin 2.4.20
- Ktor 3.6.0
- API REST
- arquitetura por domínio
- validação de entrada
- autenticação/autorização
- serialização JSON

### Persistência

- PostgreSQL
- migrations versionadas
- Exposed será adotado quando a camada de persistência for implementada
- Flyway será adotado quando a camada de migrations for implementada

### IA

A IA será acessada por uma camada de integração própria. O aplicativo não conhecerá diretamente o fornecedor do modelo.

```text
Android
  ↓
Backend API
  ↓
AI Application Service
  ↓
Context Builder
  ↓
AI Provider
```

## Compatibilidade e versionamento

- Não usar versões dinâmicas como `9.3.+` ou `latest`.
- Dependências serão centralizadas em version catalogs quando começarem a crescer.
- Atualizações de dependências serão deliberadas e acompanhadas de build/testes.

## Decisões de segurança

- Segredos nunca entram no Git.
- Configurações sensíveis vêm de ambiente/secret manager.
- O cliente não acessa o PostgreSQL diretamente.
- Toda operação financeira passa pelo backend depois que a sincronização online estiver habilitada.

## Consequência

A base fica preparada para crescimento, testes e distribuição sem transformar o aplicativo Android em um monólito que precise ser refeito quando Open Finance, IA e sincronização forem adicionados.
