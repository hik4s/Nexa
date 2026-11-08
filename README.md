# NEXA Finance

**Domínio financeiro em Kotlin e base Android para um aplicativo de finanças pessoais.**

O NEXA pretende tornar o registro de despesas rápido e a visão financeira clara. O MVP planejado reúne lançamentos manuais, contas e acompanhamento financeiro; IA contextual e Open Finance são evoluções posteriores.

**Estado atual:** fundação técnica em desenvolvimento, com domínio financeiro, migrações PostgreSQL, componentes Jetpack Compose e pipeline de testes/build. A tela Android atual é inicial e a API expõe um health check. Ainda não há uma jornada completa de cadastro e consulta de transações pelo aplicativo.

[![NEXA CI](https://github.com/hik4s/Nexa/actions/workflows/ci.yml/badge.svg)](https://github.com/hik4s/Nexa/actions/workflows/ci.yml)

## Stack

| Área | Tecnologias presentes |
| --- | --- |
| Linguagem | Kotlin |
| Android | Jetpack Compose e Material 3 |
| Backend | Ktor e kotlinx.serialization |
| Banco | PostgreSQL, migrações Flyway e driver JDBC |
| Build | Gradle Wrapper e toolchain Java 17 |
| Ambiente | Docker Compose |
| Qualidade | Testes Kotlin/JUnit e GitHub Actions |

As versões efetivas estão nos arquivos Gradle de [Android](android/app/build.gradle.kts) e [backend](backend/build.gradle.kts). O Wrapper está versionado nos dois projetos.

## O que já existe

- Modelos e regras para contas, categorias, transações, transferências, pagamentos, estornos e cancelamentos.
- Regras de parcelamento e recorrência, com cenários de calendário e consistência.
- Modelos de metas e orçamentos.
- Valores monetários representados em unidades mínimas no domínio.
- Esquema SQL versionado e tarefa de migração.
- Componentes visuais próprios: campos, botões, cards, valores monetários e feedback.
- CI com testes/build de backend, migrações PostgreSQL, testes Android, lint e geração de APK debug.

### Evidências para revisão técnica

| Tema | Código e testes |
| --- | --- |
| Valores monetários | [Money.kt](backend/src/main/kotlin/com/nexa/finance/domain/common/Money.kt) |
| Transferências consistentes | [Transfer.kt](backend/src/main/kotlin/com/nexa/finance/domain/transfer/Transfer.kt), [TransferTest.kt](backend/src/test/kotlin/com/nexa/finance/domain/transfer/TransferTest.kt) |
| Parcelamento | [Installment.kt](backend/src/main/kotlin/com/nexa/finance/domain/installment/Installment.kt), [InstallmentTest.kt](backend/src/test/kotlin/com/nexa/finance/domain/installment/InstallmentTest.kt) |
| Recorrência e calendário | [RecurrenceTest.kt](backend/src/test/kotlin/com/nexa/finance/domain/recurrence/RecurrenceTest.kt) |
| Banco | [migrações](backend/src/main/resources/db/migration), [decisões de banco](docs/database.md) |
| Android atual | [MainActivity.kt](android/app/src/main/java/com/nexa/finance/MainActivity.kt), [componentes](android/app/src/main/java/com/nexa/finance/ui/components) |

Uma transferência valida titularidade, moedas, contas distintas e consistência entre débito e crédito. O domínio é separado da interface, permitindo testar regras sem depender de uma tela ou serviço externo.

## Escopo do MVP e fronteiras atuais

| Capacidade | Estado |
| --- | --- |
| Domínio financeiro e testes | Implementado no repositório |
| Esquema e migrações | Implementados; execução depende do banco configurado |
| Componentes e tema Compose | Implementados |
| Tela inicial Android | Exibe a identificação do aplicativo |
| API | Endpoint `GET /health`; fluxos financeiros HTTP ainda pendentes |
| Persistência de casos de uso e integração Android | Próximas etapas |
| Registro rápido, dashboard e assistente de IA | Planejados, sem demonstração funcional completa |
| Sincronização e Open Finance | Evoluções posteriores |

O [plano mestre](NEXA_FINANCE_MASTER_PLAN.md) preserva a visão completa e o checkpoint operacional. Recursos planejados não são funcionalidades entregues.

## Preparar e verificar

Requer JDK 17. O build Android também requer Android SDK compatível com `compileSdk` e os componentes definidos pelo projeto.

### Backend

A partir da raiz, no PowerShell:

```powershell
cd backend
.\gradlew.bat test
.\gradlew.bat build
.\gradlew.bat run
```

O health check fica em `http://127.0.0.1:8080/health`.

Em Linux/macOS, use `./gradlew` no lugar de `.\gradlew.bat`, com permissão de execução no arquivo.

### PostgreSQL e migrações

Na raiz:

```powershell
Copy-Item .env.example .env
docker compose -f infra/docker-compose.yml up -d
```

Configure as variáveis do banco também no processo que executará o Gradle, conforme [guia de banco](docs/database.md), e execute:

```powershell
cd backend
.\gradlew.bat migrateDatabase
```

O arquivo usado pelo Docker Compose não implica carregamento automático das mesmas variáveis pelo processo Java.

### Android

Na pasta `android`:

```powershell
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:lintDebug
.\gradlew.bat :app:assembleDebug
```

O APK debug é gerado em `android/app/build/outputs/apk/debug/`. Ele corresponde à interface inicial atual, não a um aplicativo financeiro completo.

## Próximas entregas prioritárias

1. Registrar o drill de falha externa e recuperação previsto no checkpoint, com resultado reproduzível.
2. Implementar persistência e casos de uso para um recorte: conta → lançamento → saldo.
3. Expor o recorte pela API, com validação e controle de acesso.
4. Integrar uma jornada Android real, com ViewModel, estados e tratamento de falhas.
5. Adicionar testes de integração API/banco e testes da jornada Android.
6. Publicar screenshots, vídeo e APK de demonstração com dados fictícios.

A meta de registro em menos de 20 segundos é um objetivo de produto, ainda sem medição publicada. Precisão financeira e comportamento em falhas devem ser demonstrados antes de ampliar o escopo com IA.

## Documentação

- [Plano mestre e checkpoint](NEXA_FINANCE_MASTER_PLAN.md)
- [Arquitetura](docs/architecture.md)
- [Decisões de stack](docs/decisions/ADR-001-tech-stack.md)
- [Banco](docs/database.md)
- [Revisão das regras de domínio](docs/domain/domain-review-1.3.8.md)
- [Workflow de desenvolvimento](docs/development-workflow.md)
