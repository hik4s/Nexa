# NEXA Finance â€” Plano Mestre do Projeto

**VersÃ£o:** 1.0
**Status:** Documento-base do desenvolvimento

**Checkpoint atual:** Partes 1 e 2 concluídas e validadas; Parte 3 em progresso. Sincronização 3.2, discovery/consentimento Open Finance 3.3.3, audit trail técnico, logs estruturados, métricas, retries controlados, backups de produção, tracing e cache curto do cliente estão implementados e validados. Pendências reais: recuperação de desastre, conexão e importações Open Finance, segurança/compliance formal, observabilidade operacional avançada, medições de performance do dispositivo/rede e exercícios completos de falha/recuperação.
**Plataforma inicial:** Android
**Objetivo:** Construir um aplicativo de finanÃ§as pessoais sÃ³lido, seguro, escalÃ¡vel e preparado para distribuiÃ§Ã£o futura.

---

## 0. VisÃ£o do produto

O **NEXA Finance** serÃ¡ um aplicativo de gestÃ£o financeira pessoal focado em trÃªs ideias centrais:

1. **Registrar gastos em poucos segundos.**
2. **Transformar registros em uma visÃ£o financeira clara.**
3. **Usar IA para interpretar a situaÃ§Ã£o financeira e gerar insights Ãºteis.**

A integraÃ§Ã£o bancÃ¡ria via **Open Finance** serÃ¡ tratada como uma evoluÃ§Ã£o posterior. A arquitetura jÃ¡ deverÃ¡ nascer preparada para essa expansÃ£o, mas o MVP nÃ£o dependerÃ¡ dela.

### PrincÃ­pio de produto

> O usuÃ¡rio nÃ£o deve preencher manualmente informaÃ§Ãµes que o NEXA consegue inferir com seguranÃ§a.

Exemplo:

`18,90 almoÃ§o`

poderÃ¡ resultar automaticamente em:

- Valor: R$ 18,90
- Tipo: despesa
- Categoria: AlimentaÃ§Ã£o
- DescriÃ§Ã£o: AlmoÃ§o
- Data: hoje
- Conta/cartÃ£o: padrÃ£o selecionado

Meta de experiÃªncia: **registro comum em menos de 20 segundos**.

---

# 1. DecisÃµes arquiteturais

## 1.1 Stack tecnolÃ³gica base

### Android

- Kotlin
- Jetpack Compose
- Material 3, adaptado ao Design System prÃ³prio do NEXA
- MVVM + princÃ­pios de Clean Architecture
- Coroutines
- Flow
- Navigation Compose
- Testes unitÃ¡rios e instrumentados

### Backend

- Kotlin
- Ktor
- API REST
- Arquitetura modular por domÃ­nio
- ValidaÃ§Ã£o de entrada
- AutenticaÃ§Ã£o e autorizaÃ§Ã£o

### Banco de dados

- PostgreSQL
- MigraÃ§Ãµes versionadas
- Integridade referencial
- Ãndices definidos conforme os padrÃµes de consulta reais

### Infraestrutura

- Docker para desenvolvimento e ambientes reproduzÃ­veis
- VariÃ¡veis de ambiente/secrets fora do cÃ³digo
- CI/CD serÃ¡ incorporado progressivamente

### IA

- Camada de integraÃ§Ã£o desacoplada do fornecedor/modelo
- Context Builder prÃ³prio
- Regras de seguranÃ§a e filtragem de dados antes da chamada ao modelo
- Possibilidade de trocar o modelo sem reescrever o aplicativo

### IntegraÃ§Ãµes futuras

- Open Finance
- NotificaÃ§Ãµes
- Analytics/observabilidade
- ServiÃ§os de produÃ§Ã£o

---

# 2. Arquitetura geral

```text
                         NEXA Finance
                              â”‚
             â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”´â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”
             â”‚                                 â”‚
       Android App                         Backend API
             â”‚                                 â”‚
    â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”¼â”€â”€â”€â”€â”€â”€â”€â”€â”                â”Œâ”€â”€â”€â”€â”€â”€â”€â”¼â”€â”€â”€â”€â”€â”€â”€â”€â”
    â”‚        â”‚        â”‚                â”‚       â”‚        â”‚
Presentation Domain  Data           Auth   Finance    AI
    â”‚        â”‚        â”‚                â”‚       â”‚        â”‚
    â””â”€â”€â”€â”€â”€â”€â”€â”€â”´â”€â”€â”€â”€â”€â”€â”€â”€â”˜                â””â”€â”€â”€â”€â”€â”€â”€â”¼â”€â”€â”€â”€â”€â”€â”€â”€â”˜
                                              â”‚
                                         PostgreSQL
                                              â”‚
                                      IntegraÃ§Ãµes futuras
                                      â”œâ”€â”€ Open Finance
                                      â””â”€â”€ ServiÃ§os externos
```

## 2.1 Regra de separaÃ§Ã£o

A interface nÃ£o deve conter regras financeiras complexas.

Fluxo preferencial:

```text
UI
 â†“
ViewModel / State
 â†“
Use Case
 â†“
Domain
 â†“
Repository
 â†“
Data Source / API / Database
```

No backend:

```text
HTTP / Controller
 â†“
Application / Use Case
 â†“
Domain
 â†“
Repository
 â†“
PostgreSQL / External Services
```

---

# 3. Identidade visual do NEXA Finance

## 3.1 Conceito

A identidade visual deve transmitir:

- sofisticaÃ§Ã£o
- precisÃ£o
- confianÃ§a
- discriÃ§Ã£o
- inteligÃªncia
- sensaÃ§Ã£o premium
- simplicidade
- organizaÃ§Ã£o

A inspiraÃ§Ã£o estÃ©tica vem de marcas conhecidas pela **contenÃ§Ã£o visual, uso de espaÃ§o, tipografia forte e materiais de aparÃªncia premium**, mas o NEXA deverÃ¡ possuir identidade prÃ³pria e nÃ£o copiar elementos proprietÃ¡rios de nenhuma marca.

## 3.2 DireÃ§Ã£o estÃ©tica

**Minimalismo premium.**

O aplicativo deve evitar excesso de gradientes, sombras fortes, bordas desnecessÃ¡rias, Ã­cones decorativos e informaÃ§Ãµes simultÃ¢neas demais.

A interface deverÃ¡ privilegiar:

- bastante espaÃ§o em branco/negativo
- hierarquia tipogrÃ¡fica clara
- poucos elementos por tela
- microanimaÃ§Ãµes discretas
- componentes com acabamento refinado
- contraste elevado
- feedback visual sem poluiÃ§Ã£o

### Microcheckpoint 3.2.3 - conflitos de sincronização - 2026-10-05

Implementado o tratamento seguro de conflitos para transações sincronizadas. Quando uma operação offline chega com baseVersion obsoleta, o backend responde CONFLICT sem sobrescrever o estado do servidor e inclui a versão + snapshot autoritativo da transação. O Android mantém o payload local e o snapshot do servidor em uma tabela sync_conflicts, evitando descarte silencioso até existir uma ação explícita de resolução. A base Room evoluiu de V3 para V4 com migration e schema exportado.

Validação desta microetapa: teste backend direcionado de conflito passou; Android unit tests, lint e assembleDebug passaram; teste instrumentado NexaDatabaseTest passou 2/2 no AVD Nexa_API35. A UX de resolução explícita permanece como a próxima microetapa.

---

### Microcheckpoint 3.2.4 - resolução explícita de conflitos - 2026-10-05

Implementada a resolução explícita de conflitos de transação no Android. O Dashboard agora apresenta o estado local e o snapshot autoritativo do servidor e oferece duas decisões explícitas: manter servidor ou reenviar o lançamento local.

Manter servidor substitui o cache local pelo snapshot autoritativo e remove a operação conflitante da outbox. Reenviar meu lançamento atualiza baseVersion para a versão do servidor, restaura a operação para PENDING, zera tentativas, agenda o WorkManager e mantém o conflito persistido até ACK; se houver novo conflito, o snapshot é atualizado.

Validação desta microetapa: testes JVM do resolver passaram; teste instrumentado específico de resolução de conflitos com Room passou no emulador Android API 35; CI completo passou com backend (migrations/test/build) e Android (unit tests, instrumented test, lint, debug APK, release APK e release AAB). A validação não depende mais do ambiente local Android.

---

## 3.3 Paleta principal

### Obsidian

`#0B0F0D`

Uso:
- fundos escuros
- cabeÃ§alhos
- navegaÃ§Ã£o
- Ã¡reas premium

### Ivory

`#FAFAF7`

Uso:
- fundo claro principal
- superfÃ­cies
- contraste com Obsidian

### Burgundy

`#7A1F2E`

Uso:
- marca
- destaques
- aÃ§Ãµes especiais
- estados premium

### Deep Green

`#0F6B4D`

Uso:
- informaÃ§Ãµes positivas
- saldo saudÃ¡vel
- metas
- crescimento

### Soft Gray

`#94A3B8`

Uso:
- textos secundÃ¡rios
- descriÃ§Ãµes
- separadores discretos

### Champagne Accent

`#E8DCC6`

Uso extremamente moderado:
- pequenos detalhes premium
- indicadores especiais
- momentos de destaque

### Microcheckpoint 3.3.1/3.3.2 — discovery regulatório e providers — 2026-10-05

Concluído o discovery inicial para Open Finance. O escopo do NEXA permanece como agregação de dados, com arquitetura provider-neutral e sem tornar o produto um participante regulado nesta fase. Foram mapeados requisitos de consentimento, autenticação, confirmação, revogação, segurança/ciber-risco, LGPD/sigilo e limites do modelo de participação; também foi criada uma shortlist técnica de Belvo, Celcoin e Pluggy para due diligence.

O fornecedor definitivo continua em aberto até validação comercial, jurídica, de cobertura, sandbox, SLA, custos, retenção de dados, webhooks, certificação e requisitos de produção. Nenhuma credencial real ou contrato foi introduzido.

### Microcheckpoint 3.3.3 — fundação de consentimento Open Finance — 2026-10-05

Implementada e validada a fundação de consentimento provider-neutral. O backend possui máquina de estados para REQUESTED, AWAITING_AUTHORIZATION, ACTIVE, EXPIRED, REVOKED e FAILED; escopos controlados; migration V17; repositório JDBC com isolamento por usuário; contrato de adapter/provider; application service de orquestração; e nenhum token bancário é persistido.

Validação: backend test + build passou; migration V17 foi aplicada de V16 para V17 em PostgreSQL 18.6; teste de integração JDBC do consentimento passou; testes do domínio passaram. A conexão real com um provider e a jornada de autorização permanecem para a 3.3.4.

### Microcheckpoint 3.3.3 — consentimento Open Finance — 2026-10-05

Implementado o primeiro ciclo real de consentimento, sem acoplamento a provider. O backend agora persiste providerId, externalConsentId, escopos, estado, timestamps, expiração e revogação; expõe criação, listagem e revogação autenticadas por usuário; e define a interface de adapter do provider para autorização e sincronização futuras. Nenhum access token ou refresh token é armazenado no banco.

Validação desta microetapa: testes de domínio e E2E PostgreSQL adicionados; migration V17 validada pelo Flyway; CI backend/Android obrigatório antes do merge.

---

### Auditoria completa do Master Plan — 2026-10-05

Inventário atualizado contra o estado do repositório `feat/card-invoice-ledger`: 308 checks explícitos, 274 concluídos e 34 mantidos em aberto após revisão de evidência. O registro anterior de 276/32 estava desatualizado.

Correções de precisão realizadas:
- Backups de produção voltou a aberto: os scripts de backup/restore são locais e ainda não constituem a política de backup/restore do PostgreSQL de produção.
- Revogação de consentimento passou para concluído: existe rota autenticada, transição de domínio, chamada ao provider e E2E PostgreSQL com isolamento por usuário.
- Teste de filas passou para concluído: `SyncQueueTest` cobre lifecycle, recuperação de PROCESSING, backoff exponencial e validação de limites.
- Teste de integração com IA passou para concluído no escopo atual: `AiContextBuilderIntegrationTest`, `RuleBasedAiProviderTest` e `AiPanelTest` cobrem a IA interna `rules-v1`; integração com um LLM externo continua deliberadamente fora do escopo.
- Testes de falhas externas e recuperação foram reabertos: existem mecanismos de retry/recovery e scripts de restore, mas ainda não há um exercício automatizado e completo que comprove essas propriedades como requisito de produção.

Os demais checks em aberto requerem implementação interna adicional ou dependências externas reais descritas nas seções 3.1, 3.3, 3.4, 3.5, 3.6 e 3.7.
### Auditoria global do Master Plan — 2026-10-06

Revalidação realizada contra o branch `feat/card-invoice-ledger`, cabeça `003b05eca159f7908a9f0668d7454bde6d55c733`, arquivos do repositório, testes versionados, CI e runtime Railway.

| Seção | Checks | Concluídos | Abertos | Evidência principal | Status real / próxima ação |
|---|---:|---:|---:|---|---|
| 1.1–1.10 Fundação | 130 | 130 | 0 | Código, documentação e gates de CI históricos; nenhum código de fundação alterado no delta após o checkpoint de sync | CONCLUÍDO; manter regressão. |
| 2.1–2.11 Produto | 99 | 99 | 0 | Código Android/backend, testes de produto e gates de CI; delta recente não altera as telas/fluxos de produto | CONCLUÍDO; manter regressão. |
| 3.1 Backend produção | 21 | 20 | 1 | Railway API deployment `58ded56b-bb42-42ca-aba1-c135e2beed18` `SUCCESS`, PostgreSQL online, retry validado no CI | ABERTO: recuperação de desastre. |
| 3.2 Sincronização | 7 | 7 | 0 | Sync protocol, Outbox/Room, testes JVM/instrumentados e CI | CONCLUÍDO no escopo de transações. |
| 3.3 Open Finance | 13 | 5 | 8 | Consent domain/service/routes/repository, V17, E2E PostgreSQL e CI | Consentimento/revogação CONCLUÍDOS; conexão/importações/monitoramento pendentes. |
| 3.4 Segurança avançada | 10 | 7 | 3 | SQLCipher/Keystore, autorização, rate limit, audit trail V18 e testes | Fundamentos técnicos CONCLUÍDOS; faltam auditoria formal, retenção e revisão da IA. |
| 3.5 LGPD/compliance | 9 | 3 | 6 | `docs/LGPD_DATA_MAP.md`, exportação e isolamento | ABERTO: finalidade/base legal, documentos, consentimento aplicável, exclusão e retenção. |
| 3.6 Observabilidade | 8 | 3 | 5 | `Observability.kt`, /metrics protegido, /ready, X-Request-Id, tracing Railway ativo, testes e CI | Logs/métricas/tracing técnico CONCLUÍDOS; crash reporting, alertas e monitoramento operacional pendentes. |
| 3.7 Performance | 10 | 5 | 5 | paginação, índices, benchmarks de 50k registros, carga API/DB e cache curto validado por teste/CI | ABERTO: medições de inicialização/telas, memória, bateria e rede. |
| 3.8 Carga/confiabilidade | 8 | 6 | 2 | carga API/DB, SyncQueueTest, AI integration tests e sync tests | ABERTO: exercício completo de falhas externas e recuperação. |

**Resultado da auditoria:** 308 checks = **280 concluídos + 28 abertos**. Não foram encontrados checks `[x]` que precisassem ser reabertos nesta rodada; os 28 `[ ]` permanecem coerentes com as evidências disponíveis. O consentimento Open Finance 3.3.3 já está integrado ao branch atual e validado por CI; não deve ser repetido.

**CI atual:** run `37460572216` do commit `5eab0b8` concluiu verde, validando a migração de coroutines da UI. Run `37462927156` do commit `ae60ae8` concluiu verde, incluindo `ExpiringCacheTest`, backend migrations/test/build, Android unit/instrumented/lint/debug/release APK/release AAB e validação do worker de backup.

**Railway atual:** API deployment `58ded56b-bb42-42ca-aba1-c135e2beed18` e backup worker deployment `594620cb-567b-4643-8bef-ab95293f52e8` em `SUCCESS`; API e PostgreSQL estão online, sem mudanças staged atuais.

**Falhas históricas do Railway:** os 3 deployments falhos recentes ocorreram em `BUILD_IMAGE`; dois apontam para configuração antiga de root directory `/backend`. O deployment atual está saudável. Esses eventos permanecem como histórico e não justificam reabrir o runtime atual.


---

## 3.4 Regra de uso das cores

A maior parte da interface deverÃ¡ permanecer em **Obsidian + Ivory + tons neutros**.

Burgundy e Deep Green funcionam como cores de identidade e semÃ¢ntica. Champagne serÃ¡ raro.

Evitar transformar toda a interface em uma composiÃ§Ã£o multicolorida.
## 3.5 Tipografia

Prioridade:

1. legibilidade
2. hierarquia
3. personalidade discreta

Base recomendada:

- fonte sans-serif moderna
- pesos bem definidos
- nÃºmeros financeiros com destaque
- tÃ­tulos relativamente grandes e limpos
- textos secundÃ¡rios menores, porÃ©m legÃ­veis

O sistema tipogrÃ¡fico deverÃ¡ ser centralizado em tokens do Design System.

## 3.6 Forma dos componentes

- cantos moderadamente arredondados
- bordas finas quando necessÃ¡rias
- sombras quase imperceptÃ­veis
- poucos nÃ­veis de elevaÃ§Ã£o
- cards usados somente quando agregarem organizaÃ§Ã£o
- botÃµes simples e sÃ³lidos
- Ã­cones lineares e consistentes


### Auditoria incremental — observabilidade — 2026-10-06

Os checks de **Logs estruturados** e **Métricas** da seção 3.6 foram fechados após a integração da PR #15. O CI oficial validou backend migrations/test/build e Android unit tests, instrumented tests, lint, debug APK, release APK e release AAB. O desenho permanece com métricas desabilitadas por padrão e endpoint protegido por token; readiness de PostgreSQL e correlação por X-Request-Id também foram validados. Permanecem crash reporting externo, tracing, alertas, monitoramento operacional de banco/integrações e dashboards.


### Auditoria incremental — observabilidade integrada — 2026-10-06

Os checks Logs estruturados e Métricas foram promovidos para concluídos após merge da PR #15. O CI #148 validou backend migrations/test/build e Android unit tests, instrumented tests, lint, debug APK, release APK e release AAB. O endpoint /metrics permanece desabilitado por padrão e protegido por token quando habilitado; /ready valida PostgreSQL; X-Request-Id é propagado por CallId. Permanecem os itens operacionais avançados: crash reporting, tracing, alertas, monitoramento específico do banco/integrações e dashboards.

## 3.7 SensaÃ§Ã£o desejada

A tela deverÃ¡ passar a impressÃ£o de:

> â€œIsto Ã© organizado, preciso e caro, sem precisar parecer chamativo.â€

---

# 4. PrincÃ­pios permanentes do desenvolvimento

Estas regras valem para todo o projeto.

## 4.1 Microtarefas isoladas

Cada tarefa deve possuir:

- objetivo claro
- entradas
- implementaÃ§Ã£o
- testes
- revisÃ£o
- integraÃ§Ã£o
- critÃ©rio de conclusÃ£o

## 4.2 NÃ£o acumular dÃ­vida tÃ©cnica deliberadamente

Evitar:

- cÃ³digo duplicado sem justificativa
- regras de negÃ³cio espalhadas
- valores mÃ¡gicos
- dependÃªncias desnecessÃ¡rias
- arquivos gigantes
- telas responsÃ¡veis por lÃ³gica de domÃ­nio
- soluÃ§Ãµes provisÃ³rias sem registro

## 4.3 Debug contÃ­nuo

Todo erro encontrado deve ser tratado na origem sempre que possÃ­vel.

NÃ£o utilizar â€œdepois consertamosâ€ como mÃ©todo de desenvolvimento.

## 4.4 RegressÃ£o

Toda alteraÃ§Ã£o significativa deve verificar se funcionalidades anteriores continuam funcionando.

## 4.5 SeguranÃ§a desde o inÃ­cio

Dados financeiros sÃ£o sensÃ­veis. Nenhuma etapa deverÃ¡ depender de prÃ¡ticas inseguras apenas por ser â€œMVPâ€.

## 4.6 PreparaÃ§Ã£o para escala

A arquitetura deve suportar a evoluÃ§Ã£o de:

```text
1 usuÃ¡rio
â†’ 100 usuÃ¡rios
â†’ 10.000 usuÃ¡rios
â†’ escala maior
```

sem exigir reconstruÃ§Ã£o completa do produto.

---

# PARTE 1 â€” FUNDAÃ‡ÃƒO

Objetivo: construir uma base tÃ©cnica sÃ³lida antes de acelerar o desenvolvimento de funcionalidades.

---

## 1.1 â€” Fechamento tÃ©cnico

### Microtarefas

- [x] Confirmar Kotlin como linguagem principal do Android
- [x] Confirmar Kotlin + Ktor para backend
- [x] Confirmar Jetpack Compose
- [x] Confirmar MVVM + Clean Architecture
- [x] Confirmar PostgreSQL
- [x] Definir estratÃ©gia de migrations
- [x] Definir estrutura de mÃ³dulos
- [x] Definir estratÃ©gia de ambientes
- [x] Definir polÃ­tica de dependÃªncias
- [x] Definir padrÃ£o de versionamento
- [x] Definir convenÃ§Ãµes de cÃ³digo

### CritÃ©rio de conclusÃ£o

Todas as escolhas devem estar documentadas e ser suficientes para iniciar a implementaÃ§Ã£o sem decisÃµes estruturais pendentes.

---

## 1.2 â€” RepositÃ³rio e estrutura inicial

**Status:** CONCLUÍDO

### Microtarefas

- [x] Criar repositÃ³rio
- [x] Criar estrutura Android
- [x] Criar estrutura backend
- [x] Criar documentaÃ§Ã£o inicial
- [x] Configurar Git
- [x] Criar `.gitignore`
- [x] Separar configuraÃ§Ãµes locais de configuraÃ§Ãµes versionadas
- [x] Criar estratÃ©gia de branches

### CritÃ©rio de conclusÃ£o

O projeto deve clonar e iniciar de forma reprodutÃ­vel em ambiente limpo.

---

## 1.3 â€” Ambiente de desenvolvimento

**Status:** CONCLUÍDO

A toolchain operacional foi validada por CLI, emulador e infraestrutura local. Android Studio não é necessário para o fluxo validado de build/teste; o projeto permanece reproduzível via JDK + Android SDK + Gradle.

### Microtarefas

- [x] Configurar JDK/SDK necessÃ¡rios
- [x] Validar toolchain Android (CLI/emulador; IDE opcional)
- [x] Configurar Gradle
- [x] Configurar backend
- [x] Configurar Docker
- [x] Subir PostgreSQL local
- [x] Criar configuraÃ§Ã£o de ambiente local
- [x] Documentar setup

### CritÃ©rio de conclusÃ£o

Outra mÃ¡quina deve conseguir reproduzir o ambiente seguindo o README sem dependÃªncias ocultas.

---

## 1.4 â€” Design System

**Status:** CONCLUÍDO

Microtarefas concluídas: tokens de cor, tipografia, espaçamento, raio, elevação, componentes base, estados de botão, campos de entrada, cards, valores monetários, feedback/loading/error states e modos claro/escuro. Validação: assembleDebug + testDebugUnitTest + lintDebug.


### Microtarefas

- [x] Criar tokens de cor
- [x] Criar tokens tipogrÃ¡ficos
- [x] Criar tokens de espaÃ§amento
- [x] Criar tokens de raio
- [x] Criar tokens de elevaÃ§Ã£o
- [x] Criar componentes base
- [x] Criar estados de botÃ£o
- [x] Criar campos de entrada
- [x] Criar cards
- [x] Criar componentes de valor monetÃ¡rio
- [x] Criar feedback/loading/error states
- [x] Criar modo claro
- [x] Criar modo escuro
- [x] Validar acessibilidade bÃ¡sica

### CritÃ©rio de conclusÃ£o

Novas telas devem conseguir utilizar o Design System sem criar estilos ad hoc.

---

## 1.5 â€” Banco de dados e migraÃ§Ãµes

**Status:** CONCLUÍDO

### Microtarefas

- [x] Criar PostgreSQL local
- [x] Definir convenÃ§Ãµes de tabelas
- [x] Criar migrations
- [x] Definir chaves primÃ¡rias
- [x] Definir foreign keys
- [x] Definir Ã­ndices iniciais
- [x] Definir timestamps
- [x] Definir estratÃ©gia de auditoria quando necessÃ¡ria
- [x] Testar criaÃ§Ã£o do banco do zero
- [x] Testar rollback quando aplicÃ¡vel

### CritÃ©rio de conclusÃ£o

O banco Ã© reproduzÃ­vel por migrations e possui integridade referencial adequada. A recriaÃ§Ã£o local do volume vazio atÃ© V2 foi validada; a migration V3 foi validada sobre o schema V2 contra PostgreSQL 18.6, com aplicaÃ§Ã£o bem-sucedida.

A reversÃ£o adotada neste estÃ¡gio Ã© por nova migration; para o ambiente local descartÃ¡vel, a reconstruÃ§Ã£o do volume foi testada.

---

## 1.6 â€” Modelo de domÃ­nio financeiro

**Status:** CONCLUÍDO

Revisão 1.6: User, Account, Transaction, Category, PaymentMethodType, Transfer, Payment, Refund, Cancellation, Installment, Budget, Goal, RecurringTransaction, Card e Invoice estão modelados e testados. O modelo contábil de cartão usa Card como instrumento vinculado a uma Account de passivo; compras/refundos são lançados no ledger do cartão e pagamentos de fatura são eventos de duas pernas entre a conta bancária e o passivo do cartão.

### Entidades principais

- [x] User
- [x] Account
- [x] Transaction
- [x] Category
- [x] PaymentMethod
- [x] Card
- [x] Invoice
- [x] Installment
- [x] Budget
- [x] Goal
- [x] RecurringTransaction

### Microtarefas

- [x] Definir campos
- [x] Definir relacionamentos
- [x] Definir regras de estado
- [x] Definir tipos monetÃ¡rios
- [x] Definir datas e timezone
- [x] Definir regras para valores positivos/negativos
- [x] Definir transferÃªncias
- [x] Definir estornos
- [x] Definir cancelamentos
- [x] Modelar Card como instrumento vinculado a Account de passivo
- [x] Modelar Invoice e pagamentos parciais
- [x] Modelar CardPayment como evento de duas pernas
- [x] Validar ledger de compras, estornos e pagamentos de cartÃ£o
- [x] Validar migration V3 contra PostgreSQL real

### CritÃ©rio de conclusÃ£o

O domÃ­nio deve representar corretamente as operaÃ§Ãµes financeiras previstas sem depender da interface.

---

## 1.7 â€” AutenticaÃ§Ã£o e usuÃ¡rios

**Status:** CONCLUÍDO para os recursos atualmente expostos pela API; o hardening adicional de autorizaÃ§Ã£o por recurso permanece previsto na Parte 3.4.

Implementado e validado: cadastro, login, sessÃ£o persistida, logout, recuperaÃ§Ã£o de acesso, redefiniÃ§Ã£o de senha, perfil e autorizaÃ§Ã£o central. Os recursos financeiros expostos utilizam o usuÃ¡rio autenticado no fluxo de acesso; testes de isolamento HTTP reais existem para contas/transaÃ§Ãµes e fluxos financeiros principais.

A cobertura de hardening transversal por recurso serÃ¡ aprofundada na Parte 3.4 — SeguranÃ§a avanÃ§ada, sem bloquear o encerramento da Parte 2.

### Microtarefas

- [x] Criar cadastro
- [x] Criar login
- [x] Criar sessÃ£o
- [x] Criar logout
- [x] Criar recuperaÃ§Ã£o de acesso
- [x] Criar perfil bÃ¡sico
- [x] Implementar autorizaÃ§Ã£o por usuÃ¡rio
- [x] Testar isolamento de dados entre usuÃ¡rios (contas financeiras expostas)

### CritÃ©rio de conclusÃ£o

Um usuÃ¡rio nÃ£o pode acessar dados pertencentes a outro usuÃ¡rio. Para cada novo recurso financeiro exposto pela API, a consulta deve nascer escopada pelo usuÃ¡rio autenticado e possuir teste de isolamento.

---

## 1.8 â€” API base

**Status:** CONCLUÍDO

A API Ktor possui servidor, rotas, serializaÃ§Ã£o JSON, validaÃ§Ã£o de requests, tratamento central de erros, respostas padronizadas, autenticaÃ§Ã£o Bearer, documentaÃ§Ã£o inicial em docs/API.md e health check. Os fluxos de autenticaÃ§Ã£o, recuperaÃ§Ã£o, perfil, erros e contas foram testados via HTTP; os testes de integraÃ§Ã£o foram executados contra PostgreSQL real.

### Microtarefas

- [x] Criar servidor Ktor
- [x] Configurar rotas
- [x] Criar serializaÃ§Ã£o
- [x] Criar validaÃ§Ã£o de requests
- [x] Criar tratamento central de erros
- [x] Criar respostas padronizadas
- [x] Criar autenticaÃ§Ã£o
- [x] Criar documentaÃ§Ã£o inicial da API
- [x] Criar health check

### CritÃ©rio de conclusÃ£o

A API responde de maneira consistente e previsÃ­vel a requests vÃ¡lidos e invÃ¡lidos nos endpoints implementados.

---

## 1.9 â€” Motor financeiro

**Status:** CONCLUÍDO e validado pelo conjunto de testes de domínio/API e pela regressão instrumentada da Parte 2.

### Microtarefas

- [x] Cálculo de saldo
- [x] Registro de receita
- [x] Registro de despesa
- [x] TransferÃªncia entre contas
- [x] Compra no cartÃ£o
- [x] Fatura
- [x] Pagamento de fatura
- [x] Parcelamento
- [x] Estorno
- [x] Cancelamento
- [x] RecorrÃªncia

### Testes obrigatÃ³rios

- [x] Valores positivos
- [x] Valores negativos
- [x] Zero
- [x] Casas decimais
- [x] Datas diferentes
- [x] Parcelas
- [x] TransferÃªncias
- [x] Estornos
- [x] CenÃ¡rios combinados

### CritÃ©rio de conclusÃ£o

O motor deve produzir resultados determinÃ­sticos e testados para os principais fluxos financeiros.

---

## 1.10 â€” MVP tÃ©cnico

O final da Parte 1 deverÃ¡ permitir:

```text
Abrir NEXA
â†“
Criar conta / Login
â†“
Criar conta financeira
â†“
Adicionar receita
â†“
Adicionar despesa
â†“
Atualizar saldo
â†“
Consultar histÃ³rico
```

### Gate de aprovaÃ§Ã£o da Parte 1

**Status:** CONCLUÍDO e absorvido pela validação das Partes 1 e 2.

- [x] Build Android funcionando
- [x] Backend funcionando
- [x] Banco reproduzÃ­vel
- [x] AutenticaÃ§Ã£o funcionando
- [x] CRUD essencial funcionando
- [x] Motor financeiro testado
- [x] Logs bÃ¡sicos funcionando
- [x] DocumentaÃ§Ã£o atualizada
- [x] Nenhum bug crÃ­tico aberto

---

# PARTE 2 â€” PRODUTO

Objetivo: transformar a fundaÃ§Ã£o em uma experiÃªncia financeira completa e agradÃ¡vel.

---

## 2.1 â€” Registro ultrarrÃ¡pido

**Status:** CONCLUÍDO e validado no fluxo instrumentado completo.

- [x] Fluxo de criaÃ§Ã£o rÃ¡pida
- [x] Entrada numÃ©rica
- [x] DescriÃ§Ã£o curta
- [x] SeleÃ§Ã£o rÃ¡pida de categoria
- [x] Conta padrÃ£o
- [x] CartÃ£o padrÃ£o
- [x] ConfirmaÃ§Ã£o em poucos toques
- [x] Entrada em linguagem natural
- [x] Parser de valores
- [x] Parser de categoria
- [x] Parser de descriÃ§Ã£o
- [x] CorreÃ§Ã£o rÃ¡pida antes da confirmaÃ§Ã£o
- [x] Teste funcional do fluxo

O benchmark formal da meta de menos de 20 segundos permanece como mediÃ§Ã£o de performance da Parte 3.7, e nÃ£o como critÃ©rio funcional do MVP.

### Meta

Gastos comuns devem ser registrados em menos de 20 segundos.

---

## 2.2 â€” Dashboard

**Status:** CONCLUÍDO e validado no Gate 2.11.

- [x] Cálculo de saldo
- [x] Entradas
- [x] Despesas
- [x] Resultado do perÃ­odo
- [x] GrÃ¡fico por categoria
- [x] EvoluÃ§Ã£o temporal
- [x] ComparaÃ§Ã£o com perÃ­odos anteriores
- [x] Gastos recorrentes
- [x] OrÃ§amento consumido
- [x] Insights destacados

---

## 2.3 â€” Contas

- [x] Listagem
- [x] Detalhes
- [x] Cálculo de saldo
- [x] HistÃ³rico
- [x] TransferÃªncias
- [x] Conta principal
- [x] Contas secundÃ¡rias
- [x] Dinheiro em espÃ©cie

---

## 2.4 â€” CartÃµes e faturas

- [x] Cadastro de cartÃ£o
- [x] Limite
- [x] Limite disponÃ­vel
- [x] Fechamento
- [x] Vencimento
- [x] Fatura atual
- [x] PrÃ³ximas faturas
- [x] Parcelas
- [x] Pagamento
- [x] HistÃ³rico

---

## 2.5 â€” OrÃ§amentos

- [x] OrÃ§amento mensal
- [x] OrÃ§amento por categoria
- [x] Barra de progresso
- [x] Alertas
- [x] Limites personalizados
- [x] HistÃ³rico de orÃ§amento

---

## 2.6 â€” Metas financeiras

- [x] Criar meta
- [x] Valor-alvo
- [x] Valor acumulado
- [x] Prazo
- [x] Progresso
- [x] ContribuiÃ§Ãµes
- [x] ProjeÃ§Ã£o

---

## 2.7 â€” RecorrÃªncias

- [x] Criar recorrÃªncia
- [x] Editar recorrÃªncia
- [x] Pausar recorrÃªncia
- [x] Encerrar recorrÃªncia
- [x] Detectar possÃ­veis recorrÃªncias
- [x] PrÃ³ximas cobranÃ§as
- [x] Alertas

---

## 2.8 â€” Busca, filtros e histÃ³rico

- [x] Busca textual
- [x] Filtro por perÃ­odo
- [x] Filtro por categoria
- [x] Filtro por conta
- [x] Filtro por cartÃ£o
- [x] Filtro por tipo
- [x] OrdenaÃ§Ã£o
- [x] Detalhamento da transaÃ§Ã£o

---

## 2.9 â€” IA Financeira

### Arquitetura

```text
Dados financeiros
â†“
Motor de anÃ¡lise
â†“
Context Builder
â†“
Modelo de IA
â†“
Resposta estruturada
â†“
NEXA App
```

### Microtarefas

- [x] Criar interface de provider de IA
- [x] Criar camada de contexto
- [x] Definir quais dados podem ser enviados
- [x] Criar anÃ¡lise mensal
- [x] Criar explicaÃ§Ã£o de gastos
- [x] Criar detecÃ§Ã£o de padrÃµes
- [x] Criar insights
- [x] Criar perguntas financeiras
- [x] Criar anÃ¡lise de metas
- [x] Criar anÃ¡lise de orÃ§amento
- [x] Criar proteÃ§Ã£o contra respostas inventadas
- [x] Registrar origem dos nÃºmeros utilizados
- [x] Criar limites de uso

### PrincÃ­pio

A IA nÃ£o deve inventar nÃºmeros. Toda afirmaÃ§Ã£o quantitativa precisa ter origem nos dados disponÃ­veis no NEXA.

---

## 2.10 â€” NotificaÃ§Ãµes e insights proativos

- [x] Alertas de orÃ§amento
- [x] Despesas recorrentes prÃ³ximas
- [x] Fatura prÃ³xima do vencimento
- [x] Meta
- [x] AlteraÃ§Ã£o significativa de gasto
- [x] Insight periÃ³dico
- [x] ConfiguraÃ§Ãµes de notificaÃ§Ãµes

---

## 2.11 â€” Gate de aprovaÃ§Ã£o da Parte 2

**Status:** CONCLUÍDO

Validação final executada em ambiente local real: backend Ktor + PostgreSQL 18.6 + Android emulator API 35. Backend tests/build passaram; banco validado em V1–V13 com 19 tabelas, 29 índices e 36 foreign keys; Android unit tests, lint e assembleDebug passaram; regressão instrumentada passou em 9/9 testes. Nenhum bug crítico permaneceu aberto ao final do gate.

- [x] Registro rÃ¡pido validado
- [x] Dashboard consistente
- [x] Contas funcionando
- [x] CartÃµes funcionando
- [x] Faturas funcionando
- [x] OrÃ§amentos funcionando
- [x] Metas funcionando
- [x] RecorrÃªncias funcionando
- [x] IA integrada com seguranÃ§a
- [x] Testes de regressÃ£o executados
- [x] Nenhum bug crÃ­tico aberto

---

# PARTE 3 â€” ESCALA

Objetivo: preparar o NEXA para usuÃ¡rios reais, crescimento, integraÃ§Ãµes bancÃ¡rias e distribuiÃ§Ã£o.

---

## 3.1 â€” Backend de produÃ§Ã£o

**Status:** EM PROGRESSO — produção publicada e validada; runtime, containerização base, secrets, pool JDBC, banco/migrations, HTTPS, rate limiting, versionamento de API, políticas de timeout, tratamento de falhas, retries controlados e backups de produção concluídos. Permanece recuperação de desastre.

- [x] Host e porta configuráveis por ambiente
- [x] Configuração de banco por variáveis de ambiente
- [x] Validação de porta inválida (fail-fast)
- [x] Containerização do backend em JRE 17 sem root
- [x] Validação local da imagem com GET /health e usuário não-root
- [x] Contrato de secret por POSTGRES_PASSWORD_FILE
- [x] Pool JDBC compartilhado com limites configuráveis
- [x] Encerramento do pool no ciclo de vida da aplicação
- [x] Validação real do pool contra PostgreSQL local
- [x] Ambiente de produÃ§Ã£o
- [x] ConfiguraÃ§Ã£o segura de secrets
- [x] Banco de produÃ§Ã£o
- [x] Migrations de produÃ§Ã£o
- [x] HTTPS
- [x] Rate limiting
- [x] Versionamento de API
- [x] PolÃ­ticas de timeout
- [x] Retries controlados
- [x] Tratamento de falhas
- [x] Backups
- [ ] RecuperaÃ§Ã£o de desastre

### Checkpoint 3.1 — produção/rate limiting — 2026-10-04

Validado: backend publicado no Railway, PostgreSQL de produção com migrations V1–V13, HTTPS público, health check, E2E autenticado real e correção/regressão do ciclo de vida de metas. Rate limiting agora usa o plugin RateLimit do Ktor por cliente, com configuração por ambiente, `429 RATE_LIMITED`, `Retry-After` e suporte a `X-Forwarded-For` via XForwardedHeaders. Todos os endpoints de negócio estão versionados sob `/api/v1`.

Validação desta microetapa: ApplicationTest com rate limit e isolamento por cliente, `clean test build`, `scripts/verify.ps1 -SkipAndroid` e `git diff --check` passaram. O build Docker também foi concluído com sucesso no Railway, seguido de healthcheck e migrations V1–V13 sem alterações. O runtime recebeu `slf4j-simple` para eliminar o warning de ausência de provider.

---

## 3.2 â€” SincronizaÃ§Ã£o

- [x] SincronizaÃ§Ã£o de dados
- [x] Controle de versÃ£o
- [x] EstratÃ©gia offline
- [x] Retry
- [x] Estados de conflito
- [x] IdempotÃªncia
- [x] ConsistÃªncia entre dispositivos (protocolo de transações; validação de concorrência/conflicto)

### Microcheckpoint 3.2.1 — fundação local-first — 2026-10-04

Implementada e integrada a primeira camada local-first no Android com Room 2.8.5 + KSP: banco `NexaDatabase` versionado, schema exportado, `sync_operations` como Outbox por usuário, `sync_checkpoints` por usuário, provider singleton, `SessionStore` com token protegido pelo Android Keystore, agendamento via WorkManager e `SyncQueue` com lifecycle de fila e backoff exponencial limitado a 32s. O lançamento rápido passou a persistir localmente antes da sincronização; o backend aceita IDs fornecidos pelo cliente para idempotência. Operações `PROCESSING` antigas são recuperadas automaticamente após interrupção do worker.

O protocolo de sincronização servidor↔dispositivo ainda não foi ligado; por isso o item principal de sincronização permanece aberto. Esta etapa cobre somente a fundação persistente, sessão segura e outbox/retry.

Validação: backend `clean test` passou; Android `testDebugUnitTest` + `assembleDebug` passaram; teste instrumentado da sessão segura passou 2/2; fluxo E2E autenticado afetado pela alteração passou 1/1 com Ktor + PostgreSQL + emulador API 35 ativos; `git diff --check` passou.

### Microcheckpoint 3.2.2 - protocolo servidor↔dispositivo - 2026-10-04

Implementado o primeiro protocolo real de sincronização para transações: endpoint autenticado `POST /api/v1/sync/push` com lotes de até 50 operações, endpoint `GET /api/v1/sync/pull?cursor=&limit=` com cursor monotônico, idempotência por UUID de transação, acknowledgements por operação e persistência de mudanças no servidor. A migration V14 cria a sequência/catálogo `sync_changes` e trigger de auditoria para INSERT/UPDATE/DELETE em transações.

No Android, o Outbox passou a fazer push em lote, remover somente operações reconhecidas, executar pull paginado, persistir transações recebidas em `local_transactions` e avançar o checkpoint somente após aplicar as mudanças. A base Room evoluiu de V1 para V2 com migration explícita. O worker real foi validado contra Ktor + PostgreSQL, incluindo push, pull, checkpoint e esvaziamento do Outbox.

Escopo deliberadamente limitado a transações. Conflitos entre dispositivos e sincronização dos demais agregados permanecem para a próxima microetapa.

Validação: migration V14 aplicada e validada pelo Flyway; teste backend específico de push/pull/idempotência passou; Android `assembleDebug` passou; worker instrumentado real passou 1/1; fluxo visual de lançamento rápido + sincronização remota passou 1/1; `git diff --check` passou. A suíte backend completa e `testDebugUnitTest` tiveram execuções adicionais interrompidas por processos Gradle órfãos do ambiente, sem falha funcional correspondente; as verificações específicas determinísticas permaneceram verdes.

---

## 3.3 â€” Open Finance

A integraÃ§Ã£o bancÃ¡ria serÃ¡ adicionada somente quando a base financeira e a infraestrutura estiverem maduras.

### Microtarefas

- [x] Levantar requisitos regulatÃ³rios aplicÃ¡veis
- [x] Mapear fornecedores/participantes de integraÃ§Ã£o
- [x] Definir arquitetura de integraÃ§Ã£o
- [x] Implementar consentimento
- [ ] Implementar conexÃ£o
- [ ] Importar contas
- [ ] Importar transaÃ§Ãµes
- [ ] Importar cartÃµes quando suportado
- [ ] Categorizar transaÃ§Ãµes
- [ ] Criar conciliaÃ§Ã£o
- [ ] Tratar duplicidades
- [x] Tratar revogaÃ§Ã£o de consentimento
- [ ] Monitorar sincronizaÃ§Ã£o

---

## 3.4 â€” SeguranÃ§a avanÃ§ada

- [x] ProteÃ§Ã£o de credenciais
- [x] ProteÃ§Ã£o de tokens
- [x] Armazenamento seguro no dispositivo
- [x] Criptografia quando necessÃ¡ria
- [x] AutorizaÃ§Ã£o por recurso
- [x] ProteÃ§Ã£o de endpoints
- [x] DetecÃ§Ã£o de abuso
- [ ] Auditoria
- [ ] PolÃ­tica de retenÃ§Ã£o de dados
- [ ] RevisÃ£o de exposiÃ§Ã£o de dados da IA


**Checkpoint 3.4.1 — criptografia do banco local — 2026-10-05**

Concluído e validado. O banco Room `nexa-local.db` agora usa SQLCipher 4.19 com chave aleatória de 256 bits protegida por Android Keystore + AES-GCM. A abertura do banco ocorre somente após a preparação/validação da chave; bases SQLite em texto são convertidas para o formato criptografado antes de o Room abrir o arquivo. A migração preserva os dados e `user_version`, possui recuperação de interrupção e remove o backup plaintext somente após validação de integridade do banco criptografado.

Validação executada: bateria instrumentada crítica de criptografia/provedor 3/3; suíte instrumentada completa 16/16; testes unitários, lint, assembleRelease e bundleRelease passaram. A implementação foi integrada no commit `553ce6ba0c7137129d52c8e1f4d75e6099dfceb2`. O item de criptografia em repouso deixa de ser bloqueador; permanecem auditoria, retenção de dados e revisão de exposição da IA.
---

### Microcheckpoint 3.4.2 — audit trail de eventos críticos — 2026-10-05

Implementada a fundação técnica de auditoria para eventos críticos. A migration V18 cria `audit_events`; o recorder mantém o evento independente do ciclo de vida do usuário e não armazena senhas, tokens, recovery tokens ou URLs de autorização. O ciclo Open Finance registra criação, início de autorização, mudanças de status e revogação; autenticação registra cadastro, login bem-sucedido/falho, logout e recuperação/reset; exportação de dados registra a solicitação.

Validação adicionada: teste JDBC do repositório e testes JVM do recorder. A cobertura completa de auditoria de todas as mutações financeiras e a política de retenção continuam abertas.

---

## 3.5 â€” LGPD e compliance

- [x] Mapear dados pessoais
- [x] Mapear dados sensÃ­veis/financeiros tratados
- [ ] Definir finalidade de tratamento
- [ ] PolÃ­tica de privacidade
- [ ] Termos de uso
- [ ] GestÃ£o de consentimentos quando aplicÃ¡vel
- [x] ExportaÃ§Ã£o de dados
- [ ] ExclusÃ£o de conta/dados conforme requisitos aplicÃ¡veis
- [ ] RetenÃ§Ã£o e descarte

---

## 3.6 â€” Observabilidade

- [x] Logs estruturados
- [x] MÃ©tricas
- [ ] Crash reporting
- [x] Tracing quando necessÃ¡rio
- [ ] Alertas
- [ ] Monitoramento de API
- [ ] Monitoramento do banco
- [ ] Monitoramento de integraÃ§Ãµes externas

---

## 3.7 â€” Performance

- [ ] Tempo de inicializaÃ§Ã£o
- [ ] Tempo de abertura de telas
- [ ] Uso de memÃ³ria
- [ ] Uso de bateria
- [ ] Uso de rede
- [x] PaginaÃ§Ã£o
- [x] Cache
- [x] Consultas do banco
- [x] Ãndices
- [x] Teste com grande volume de transaÃ§Ãµes

### CenÃ¡rio-alvo de teste

O aplicativo deve continuar utilizÃ¡vel com histÃ³rico financeiro grande, incluindo milhares de transaÃ§Ãµes por usuÃ¡rio.

### Microcheckpoint 3.7.1 â€” consultas, Ã­ndices e grande volume â€” 2026-10-04

O histÃ³rico de transaÃ§Ãµes foi otimizado para volume alto. A migration V16 substitui idx_transactions_account_date por idx_transactions_account_date_created_id (account_id, occurred_on DESC, created_at DESC, id DESC) e adiciona idx_transactions_related_type_active para operaÃ§Ãµes relacionadas nÃ£o canceladas. A listagem global sem accountId passou a limitar candidatos por conta via CROSS JOIN LATERAL, reduzindo o conjunto que precisa ser ordenado antes da paginaÃ§Ã£o final.

ValidaÃ§Ã£o de grande volume: scripts/benchmark-transactions.ps1 inseriu 50.000 transaÃ§Ãµes sintÃ©ticas distribuÃ­das em cinco contas de performance dentro de uma transaÃ§Ã£o revertida. O EXPLAIN ANALYZE retornou 100 linhas em 1,806 ms, usando o novo idx_transactions_account_date_created_id, com aproximadamente 501 candidatos e top-N heapsort de 63 kB. Nenhum dado permanente foi deixado no banco.

RegressÃ£o: TransactionApiTest passou isoladamente com os fluxos de paginaÃ§Ã£o, ediÃ§Ã£o com versionamento e sincronizaÃ§Ã£o; a suÃ­te backend completa fechou com 181 testes verdes usando pool de teste 2/0. O Gradle agora aplica esses limites somente como padrÃ£o do task de testes, preservando override explÃ­cito por ambiente.

Escopo concluÃ­do neste checkpoint: consultas do banco, Ã­ndices e teste com grande volume; concorrÃªncia e carga da API foram validadas no checkpoint 3.8.1. InicializaÃ§Ã£o, abertura de telas, memÃ³ria, bateria, rede e cache continuam pendentes; filas, integraÃ§Ã£o com IA, sincronizaÃ§Ã£o, falhas externas e recuperaÃ§Ã£o seguem no bloco 3.8.

---

## 3.8 â€” Testes de carga e confiabilidade

- [x] Teste de concorrÃªncia
- [x] Teste de carga da API
- [x] Teste de banco
- [x] Teste de filas quando existirem
- [x] Teste de integraÃ§Ã£o com IA
- [x] Teste de sincronizaÃ§Ã£o
- [x] Teste de falhas externas
- [x] Teste de recuperaÃ§Ã£o

### Checkpoint 3.8.1 — carga concorrente da API

Validação real executada em 2026-10-04 com o harness `scripts/load-test-transactions.ps1`:

- Seed: 5.000 transações sintéticas isoladas.
- Carga: 500 requisições autenticadas para `GET /api/v1/transactions?limit=100&offset=0`.
- Concorrência: 25 workers.
- Sucesso: 500/500.
- Falha: 0.
- Duração da carga: 1.260 ms.
- RPS observado: 396,71.
- P50: 36 ms.
- P95: 112 ms.
- P99: 181 ms.
- Cleanup confirmado: 0 usuários, 0 contas, 0 transações e 0 `sync_changes` residuais.

Observação: o backend foi iniciado para o teste com `RATE_LIMIT_REQUESTS_PER_MINUTE=10000`, preservando a configuração padrão de produção (120/min por IP) fora deste cenário. Isso isola capacidade/concorrência de API da limitação de tráfego intencional.

### Checkpoint 3.8.2 — carga concorrente do PostgreSQL

Validação real executada em 2026-10-04 com o harness `scripts/benchmark-database.ps1`, usando PostgreSQL 18.6 via Docker e `pgbench`:

- Dataset isolado: 5 contas sintéticas, 10.000 transações por conta, 50.000 transações.
- Consulta: mesma estratégia global de histórico do `JdbcTransactionRepository`, com `CROSS JOIN LATERAL`, índice por conta/data e página de 100 registros.
- Concorrência: 25 clientes, 5 threads.
- Duração: 15 s.
- Transações processadas: 60.521.
- Falhas: 0 (0,000%).
- TPS: 4.035,98.
- Latência média: 5,927 ms.
- Desvio padrão: 1,456 ms.
- Tempo inicial de conexão: 24,520 ms.
- Cleanup confirmado: dataset sintético removido ao final da execução.

### Auditoria incremental — resiliência, backups e tracing — 2026-10-06

Os checks **Retries controlados** e **Backups** foram fechados após implementação, testes e validação em produção. O backend passou migrations, testes e build no CI; o retry usa backoff exponencial com limites explícitos de 3 tentativas, 250 ms inicial e teto de 2 s, aplicado apenas a operações externas repetíveis. O worker de backup PostgreSQL foi validado por sintaxe/build no CI e executou um cron real com sucesso, incluindo `pg_dump`, upload ao Bucket privado e verificação do objeto.

O check **Tracing quando necessário** foi fechado: o tracing e a auto-instrumentação nativa da API estão ativos em production. Ainda não foram observados traces no período de validação por ausência de tráfego, portanto tracing operacional avançado e alertas continuam em escopo separado.

Permanecem abertos retenção e recuperação de desastre: o Railway Bucket atual não fornece versionamento/lifecycle, e esses requisitos precisam de política própria e exercício de restauração.



### Checkpoint 3.8.3 — falhas externas e recuperação — 2026-10-06

Validação automatizada concluída no run CI #171, commit `fc1f9a047f8c6f1eb2fd0cef9314fc3af6fc68cf`, com backend migrations, testes e build verdes; Android unitário, instrumentado, lint, debug APK, release APK e release AAB verdes; e validação do backup worker verde.

Foram adicionados dois cenários explícitos ao `OpenFinanceConsentServiceTest`:
- falha transitória externa que esgota o retry configurado e preserva o consentimento em `AWAITING_AUTHORIZATION`;
- recuperação do provider externo, com nova tentativa bem-sucedida que leva o consentimento a `ACTIVE`.

A prova cobre comportamento determinístico de falha/retry/recuperação da dependência externa. Isso não encerra recuperação de desastre do banco/produção, que permanece separada no check 3.1 e exige exercício de restauração operacional.


### Próxima execução oficial — 3.1 Recuperação de desastre

O exercício 3.8.3 comprovou falha/retry/recuperação de uma dependência externa, mas não substitui o exercício de restauração do PostgreSQL de produção. O próximo objetivo é executar e documentar uma restauração real de um backup de produção em ambiente isolado, verificar integridade pós-restore e registrar RPO/RTO observados.

**Critério de avanço:** backup real identificado, restauração isolada concluída sem alterar a produção, verificações de integridade executadas, resultado documentado e procedimento reproduzível.
