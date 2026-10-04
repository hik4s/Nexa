# NEXA Finance

Aplicativo Android de finanças pessoais com registro ultrarrápido, visão financeira e IA contextual.

## Status

**Parte 2 — Produto concluída e validada. Próxima etapa: Parte 3 — 3.1 Backend de produção.**

O Gate 2.11 foi fechado com backend, PostgreSQL, Android build/lint/unit tests e 9/9 testes instrumentados passando em ambiente local real. O repositório está em uma branch de desenvolvimento dedicada; CI remoto deve ser considerado separado da validação local.

## Stack fixada

| Área | Tecnologia |
|---|---|
| Android | Kotlin 2.4.20 + Jetpack Compose |
| Android build | AGP 9.3.1 + Gradle 9.5.0 |
| Android UI | Compose BOM 2026.08.00 + Material 3 |
| Navegação | Navigation Compose 2.10.2 |
| Activity | activity-compose 1.12.4 |
| Backend | Kotlin 2.4.20 + Ktor 3.6.0 |
| Banco | PostgreSQL |
| ORM/SQL | JDBC + SQL explícito |
| Migrações | Flyway 13.9.0 |
| Runtime Java | JDK 17 (toolchain) |
| Infra local | Docker Compose |

O AGP 9.3 requer no mínimo Gradle 9.5 e suporta até API 37; JDK 17 é o JDK de referência dessa versão. citeturn160169search1turn160169search3

## Identidade visual

- Obsidian `#0B0B0C`
- Ivory `#F5F2EA`
- Burgundy `#5A1D2A`
- Deep Green `#173B2B`
- Soft Gray `#8C8C8C`
- Champagne `#C7B27A` (uso mínimo)

Direção: minimalismo premium, alto contraste, espaço negativo, tipografia limpa, ícones lineares e microinterações discretas.

## Estrutura

```text
nexa-finance/
├── android/        # Aplicativo Android
├── backend/        # API Ktor
├── docs/           # Arquitetura, decisões e processos
├── infra/          # Infraestrutura local
├── scripts/        # Automação de desenvolvimento
├── .env.example
└── README.md
```

## Regra de desenvolvimento

Cada microtarefa passa por implementação → teste → debug → revisão → refatoração → integração → checkpoint.

Nenhum bloqueio ou requisito ausente será ocultado. Se algo não puder ser validado no ambiente disponível, isso será registrado explicitamente como bloqueio.

## Ambiente local

O Android e o Backend exigem JDK 17 para o build. O PostgreSQL local é provisionado via Docker Compose em `infra/docker-compose.yml`.

Para a infraestrutura local:

```powershell
Copy-Item .env.example .env
docker compose -f .\infra\docker-compose.yml up -d
docker compose -f .\infra\docker-compose.yml ps
```
