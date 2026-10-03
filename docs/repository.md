# NEXA Finance — Repositório

## Objetivo

O NEXA Finance usa um monorepo Git com dois projetos Gradle independentes:

```text
nexa-finance/
├── android/   # cliente Android
├── backend/   # API Ktor
├── docs/      # decisões e documentação
├── infra/     # infraestrutura local
└── scripts/   # automações de desenvolvimento
```

### Por que dois builds Gradle?

Android e backend evoluem com ciclos de build diferentes. Mantê-los independentes reduz acoplamento e permite que cada lado tenha seu próprio catálogo de dependências, toolchain e tarefas.

## Regras de organização

- `android/`: somente código e configuração do aplicativo Android.
- `backend/`: somente código e configuração da API.
- `docs/`: arquitetura, decisões (ADR), processos e checkpoints.
- `infra/`: somente recursos de infraestrutura local/ambientes.
- `scripts/`: automações repetíveis que não pertencem ao domínio da aplicação.
- Configurações locais e segredos ficam fora do Git.

## Convenções Git

Branches principais:

- `main`: estado estável e potencialmente distribuível.
- `develop`: integração contínua do trabalho aprovado, enquanto o projeto estiver em fase inicial.

Branches de trabalho:

```text
feature/<dominio>-<objetivo>
fix/<dominio>-<problema>
chore/<area>-<objetivo>
refactor/<area>-<objetivo>
```

Commits devem ser pequenos, objetivos e descrever a mudança no imperativo. Exemplo:

```text
feat(finance): add transaction domain model
fix(auth): reject expired refresh token
chore(build): pin kotlin plugin version
```

## Regra para arquivos gerados

Builds, caches e configurações específicas da máquina não entram no Git. Os arquivos necessários para reproduzir o build, incluindo o Gradle Wrapper, devem ser versionados quando gerados.
