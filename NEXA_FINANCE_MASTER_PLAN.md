# NEXA Finance â€” Plano Mestre do Projeto

**VersÃ£o:** 1.0
**Status:** Documento-base do desenvolvimento

**Checkpoint atual:** Parte 1 - Microtarefas 1.3.1 a 1.3.8 concluidas; 1.4 em andamento, com tokens de cor, tipografia, espaçamento, raio, elevação, componentes base e estados de botão implementados e validados; automacao de desenvolvimento instalada e verificada.
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

## 3.3 Paleta principal

### Obsidian

`#0B0B0C`

Uso:
- fundos escuros
- cabeÃ§alhos
- navegaÃ§Ã£o
- Ã¡reas premium

### Ivory

`#F5F2EA`

Uso:
- fundo claro principal
- superfÃ­cies
- contraste com Obsidian

### Burgundy

`#5A1D2A`

Uso:
- marca
- destaques
- aÃ§Ãµes especiais
- estados premium

### Deep Green

`#173B2B`

Uso:
- informaÃ§Ãµes positivas
- saldo saudÃ¡vel
- metas
- crescimento

### Soft Gray

`#8C8C8C`

Uso:
- textos secundÃ¡rios
- descriÃ§Ãµes
- separadores discretos

### Champagne Accent

`#C7B27A`

Uso extremamente moderado:
- pequenos detalhes premium
- indicadores especiais
- momentos de destaque

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

### Microtarefas

- [ ] Criar repositÃ³rio
- [ ] Criar estrutura Android
- [ ] Criar estrutura backend
- [ ] Criar documentaÃ§Ã£o inicial
- [ ] Configurar Git
- [ ] Criar `.gitignore`
- [ ] Separar configuraÃ§Ãµes locais de configuraÃ§Ãµes versionadas
- [ ] Criar estratÃ©gia de branches

### CritÃ©rio de conclusÃ£o

O projeto deve clonar e iniciar de forma reprodutÃ­vel em ambiente limpo.

---

## 1.3 â€” Ambiente de desenvolvimento

### Microtarefas

- [ ] Configurar JDK/SDK necessÃ¡rios
- [ ] Configurar Android Studio
- [ ] Configurar Gradle
- [ ] Configurar backend
- [ ] Configurar Docker
- [ ] Subir PostgreSQL local
- [ ] Criar configuraÃ§Ã£o de ambiente local
- [ ] Documentar setup

### CritÃ©rio de conclusÃ£o

Outra mÃ¡quina deve conseguir reproduzir o ambiente seguindo o README sem dependÃªncias ocultas.

---

## 1.4 â€” Design System

**Status:** EM ANDAMENTO

Microtarefas concluídas: tokens de cor, tipografia, espaçamento, raio, elevação, componentes base e estados de botão. Validação: assembleDebug.


### Microtarefas

- [x] Criar tokens de cor
- [x] Criar tokens tipogrÃ¡ficos
- [x] Criar tokens de espaÃ§amento
- [x] Criar tokens de raio
- [x] Criar tokens de elevaÃ§Ã£o
- [x] Criar componentes base
- [x] Criar estados de botÃ£o
- [ ] Criar campos de entrada
- [ ] Criar cards
- [ ] Criar componentes de valor monetÃ¡rio
- [ ] Criar feedback/loading/error states
- [ ] Criar modo claro
- [ ] Criar modo escuro
- [ ] Validar acessibilidade bÃ¡sica

### CritÃ©rio de conclusÃ£o

Novas telas devem conseguir utilizar o Design System sem criar estilos ad hoc.

---

## 1.5 â€” Banco de dados e migraÃ§Ãµes

### Microtarefas

- [ ] Criar PostgreSQL local
- [ ] Definir convenÃ§Ãµes de tabelas
- [ ] Criar migrations
- [ ] Definir chaves primÃ¡rias
- [ ] Definir foreign keys
- [ ] Definir Ã­ndices iniciais
- [ ] Definir timestamps
- [ ] Definir estratÃ©gia de auditoria quando necessÃ¡ria
- [ ] Testar criaÃ§Ã£o do banco do zero
- [ ] Testar rollback quando aplicÃ¡vel

### CritÃ©rio de conclusÃ£o

O banco deve ser reproduzÃ­vel por migrations e possuir integridade referencial adequada.

---

## 1.6 â€” Modelo de domÃ­nio financeiro

### Entidades principais

- [ ] User
- [ ] Account
- [ ] Transaction
- [ ] Category
- [ ] PaymentMethod
- [ ] Card
- [ ] Invoice
- [ ] Installment
- [ ] Budget
- [ ] Goal
- [ ] RecurringTransaction

### Microtarefas

- [ ] Definir campos
- [ ] Definir relacionamentos
- [ ] Definir regras de estado
- [ ] Definir tipos monetÃ¡rios
- [ ] Definir datas e timezone
- [ ] Definir regras para valores positivos/negativos
- [ ] Definir transferÃªncias
- [ ] Definir estornos
- [ ] Definir cancelamentos

### CritÃ©rio de conclusÃ£o

O domÃ­nio deve representar corretamente as operaÃ§Ãµes financeiras previstas sem depender da interface.

---

## 1.7 â€” AutenticaÃ§Ã£o e usuÃ¡rios

### Microtarefas

- [ ] Criar cadastro
- [ ] Criar login
- [ ] Criar sessÃ£o
- [ ] Criar logout
- [ ] Criar recuperaÃ§Ã£o de acesso
- [ ] Criar perfil bÃ¡sico
- [ ] Implementar autorizaÃ§Ã£o por usuÃ¡rio
- [ ] Testar isolamento de dados entre usuÃ¡rios

### CritÃ©rio de conclusÃ£o

Um usuÃ¡rio nÃ£o pode acessar dados pertencentes a outro usuÃ¡rio.

---

## 1.8 â€” API base

### Microtarefas

- [ ] Criar servidor Ktor
- [ ] Configurar rotas
- [ ] Criar serializaÃ§Ã£o
- [ ] Criar validaÃ§Ã£o de requests
- [ ] Criar tratamento central de erros
- [ ] Criar respostas padronizadas
- [ ] Criar autenticaÃ§Ã£o
- [ ] Criar documentaÃ§Ã£o inicial da API
- [ ] Criar health check

### CritÃ©rio de conclusÃ£o

A API deve responder de maneira consistente e previsÃ­vel a requests vÃ¡lidos e invÃ¡lidos.

---

## 1.9 â€” Motor financeiro

### Microtarefas

- [ ] CÃ¡lculo de saldo
- [ ] Registro de receita
- [ ] Registro de despesa
- [ ] TransferÃªncia entre contas
- [ ] Compra no cartÃ£o
- [ ] Fatura
- [ ] Pagamento de fatura
- [ ] Parcelamento
- [ ] Estorno
- [ ] Cancelamento
- [ ] RecorrÃªncia

### Testes obrigatÃ³rios

- [ ] Valores positivos
- [ ] Valores negativos
- [ ] Zero
- [ ] Casas decimais
- [ ] Datas diferentes
- [ ] Parcelas
- [ ] TransferÃªncias
- [ ] Estornos
- [ ] CenÃ¡rios combinados

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

- [ ] Build Android funcionando
- [ ] Backend funcionando
- [ ] Banco reproduzÃ­vel
- [ ] AutenticaÃ§Ã£o funcionando
- [ ] CRUD essencial funcionando
- [ ] Motor financeiro testado
- [ ] Logs bÃ¡sicos funcionando
- [ ] DocumentaÃ§Ã£o atualizada
- [ ] Nenhum bug crÃ­tico aberto

---

# PARTE 2 â€” PRODUTO

Objetivo: transformar a fundaÃ§Ã£o em uma experiÃªncia financeira completa e agradÃ¡vel.

---

## 2.1 â€” Registro ultrarrÃ¡pido

- [ ] Fluxo de criaÃ§Ã£o rÃ¡pida
- [ ] Entrada numÃ©rica
- [ ] DescriÃ§Ã£o curta
- [ ] SeleÃ§Ã£o rÃ¡pida de categoria
- [ ] Conta padrÃ£o
- [ ] CartÃ£o padrÃ£o
- [ ] ConfirmaÃ§Ã£o em poucos toques
- [ ] Entrada em linguagem natural
- [ ] Parser de valores
- [ ] Parser de categoria
- [ ] Parser de descriÃ§Ã£o
- [ ] CorreÃ§Ã£o rÃ¡pida antes da confirmaÃ§Ã£o
- [ ] Teste de tempo de execuÃ§Ã£o do fluxo

### Meta

Gastos comuns devem ser registrados em menos de 20 segundos.

---

## 2.2 â€” Dashboard

- [ ] Saldo
- [ ] Entradas
- [ ] Despesas
- [ ] Resultado do perÃ­odo
- [ ] GrÃ¡fico por categoria
- [ ] EvoluÃ§Ã£o temporal
- [ ] ComparaÃ§Ã£o com perÃ­odos anteriores
- [ ] Gastos recorrentes
- [ ] OrÃ§amento consumido
- [ ] Insights destacados

---

## 2.3 â€” Contas

- [ ] Listagem
- [ ] Detalhes
- [ ] Saldo
- [ ] HistÃ³rico
- [ ] TransferÃªncias
- [ ] Conta principal
- [ ] Contas secundÃ¡rias
- [ ] Dinheiro em espÃ©cie

---

## 2.4 â€” CartÃµes e faturas

- [ ] Cadastro de cartÃ£o
- [ ] Limite
- [ ] Limite disponÃ­vel
- [ ] Fechamento
- [ ] Vencimento
- [ ] Fatura atual
- [ ] PrÃ³ximas faturas
- [ ] Parcelas
- [ ] Pagamento
- [ ] HistÃ³rico

---

## 2.5 â€” OrÃ§amentos

- [ ] OrÃ§amento mensal
- [ ] OrÃ§amento por categoria
- [ ] Barra de progresso
- [ ] Alertas
- [ ] Limites personalizados
- [ ] HistÃ³rico de orÃ§amento

---

## 2.6 â€” Metas financeiras

- [ ] Criar meta
- [ ] Valor-alvo
- [ ] Valor acumulado
- [ ] Prazo
- [ ] Progresso
- [ ] ContribuiÃ§Ãµes
- [ ] ProjeÃ§Ã£o

---

## 2.7 â€” RecorrÃªncias

- [ ] Criar recorrÃªncia
- [ ] Editar recorrÃªncia
- [ ] Pausar recorrÃªncia
- [ ] Encerrar recorrÃªncia
- [ ] Detectar possÃ­veis recorrÃªncias
- [ ] PrÃ³ximas cobranÃ§as
- [ ] Alertas

---

## 2.8 â€” Busca, filtros e histÃ³rico

- [ ] Busca textual
- [ ] Filtro por perÃ­odo
- [ ] Filtro por categoria
- [ ] Filtro por conta
- [ ] Filtro por cartÃ£o
- [ ] Filtro por tipo
- [ ] OrdenaÃ§Ã£o
- [ ] Detalhamento da transaÃ§Ã£o

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

- [ ] Criar interface de provider de IA
- [ ] Criar camada de contexto
- [ ] Definir quais dados podem ser enviados
- [ ] Criar anÃ¡lise mensal
- [ ] Criar explicaÃ§Ã£o de gastos
- [ ] Criar detecÃ§Ã£o de padrÃµes
- [ ] Criar insights
- [ ] Criar perguntas financeiras
- [ ] Criar anÃ¡lise de metas
- [ ] Criar anÃ¡lise de orÃ§amento
- [ ] Criar proteÃ§Ã£o contra respostas inventadas
- [ ] Registrar origem dos nÃºmeros utilizados
- [ ] Criar limites de uso

### PrincÃ­pio

A IA nÃ£o deve inventar nÃºmeros. Toda afirmaÃ§Ã£o quantitativa precisa ter origem nos dados disponÃ­veis no NEXA.

---

## 2.10 â€” NotificaÃ§Ãµes e insights proativos

- [ ] Alertas de orÃ§amento
- [ ] Despesas recorrentes prÃ³ximas
- [ ] Fatura prÃ³xima do vencimento
- [ ] Meta
- [ ] AlteraÃ§Ã£o significativa de gasto
- [ ] Insight periÃ³dico
- [ ] ConfiguraÃ§Ãµes de notificaÃ§Ãµes

---

## 2.11 â€” Gate de aprovaÃ§Ã£o da Parte 2

- [ ] Registro rÃ¡pido validado
- [ ] Dashboard consistente
- [ ] Contas funcionando
- [ ] CartÃµes funcionando
- [ ] Faturas funcionando
- [ ] OrÃ§amentos funcionando
- [ ] Metas funcionando
- [ ] RecorrÃªncias funcionando
- [ ] IA integrada com seguranÃ§a
- [ ] Testes de regressÃ£o executados
- [ ] Nenhum bug crÃ­tico aberto

---

# PARTE 3 â€” ESCALA

Objetivo: preparar o NEXA para usuÃ¡rios reais, crescimento, integraÃ§Ãµes bancÃ¡rias e distribuiÃ§Ã£o.

---

## 3.1 â€” Backend de produÃ§Ã£o

- [ ] Ambiente de produÃ§Ã£o
- [ ] ConfiguraÃ§Ã£o segura de secrets
- [ ] Banco de produÃ§Ã£o
- [ ] Migrations de produÃ§Ã£o
- [ ] HTTPS
- [ ] Rate limiting
- [ ] Versionamento de API
- [ ] PolÃ­ticas de timeout
- [ ] Retries controlados
- [ ] Tratamento de falhas
- [ ] Backups
- [ ] RecuperaÃ§Ã£o de desastre

---

## 3.2 â€” SincronizaÃ§Ã£o

- [ ] SincronizaÃ§Ã£o de dados
- [ ] Controle de versÃ£o
- [ ] EstratÃ©gia offline
- [ ] Retry
- [ ] Estados de conflito
- [ ] IdempotÃªncia
- [ ] ConsistÃªncia entre dispositivos

---

## 3.3 â€” Open Finance

A integraÃ§Ã£o bancÃ¡ria serÃ¡ adicionada somente quando a base financeira e a infraestrutura estiverem maduras.

### Microtarefas

- [ ] Levantar requisitos regulatÃ³rios aplicÃ¡veis
- [ ] Mapear fornecedores/participantes de integraÃ§Ã£o
- [ ] Definir arquitetura de integraÃ§Ã£o
- [ ] Implementar consentimento
- [ ] Implementar conexÃ£o
- [ ] Importar contas
- [ ] Importar transaÃ§Ãµes
- [ ] Importar cartÃµes quando suportado
- [ ] Categorizar transaÃ§Ãµes
- [ ] Criar conciliaÃ§Ã£o
- [ ] Tratar duplicidades
- [ ] Tratar revogaÃ§Ã£o de consentimento
- [ ] Monitorar sincronizaÃ§Ã£o

---

## 3.4 â€” SeguranÃ§a avanÃ§ada

- [ ] ProteÃ§Ã£o de credenciais
- [ ] ProteÃ§Ã£o de tokens
- [ ] Armazenamento seguro no dispositivo
- [ ] Criptografia quando necessÃ¡ria
- [ ] AutorizaÃ§Ã£o por recurso
- [ ] ProteÃ§Ã£o de endpoints
- [ ] DetecÃ§Ã£o de abuso
- [ ] Auditoria
- [ ] PolÃ­tica de retenÃ§Ã£o de dados
- [ ] RevisÃ£o de exposiÃ§Ã£o de dados da IA

---

## 3.5 â€” LGPD e compliance

- [ ] Mapear dados pessoais
- [ ] Mapear dados sensÃ­veis/financeiros tratados
- [ ] Definir finalidade de tratamento
- [ ] PolÃ­tica de privacidade
- [ ] Termos de uso
- [ ] GestÃ£o de consentimentos quando aplicÃ¡vel
- [ ] ExportaÃ§Ã£o de dados
- [ ] ExclusÃ£o de conta/dados conforme requisitos aplicÃ¡veis
- [ ] RetenÃ§Ã£o e descarte

---

## 3.6 â€” Observabilidade

- [ ] Logs estruturados
- [ ] MÃ©tricas
- [ ] Crash reporting
- [ ] Tracing quando necessÃ¡rio
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
- [ ] PaginaÃ§Ã£o
- [ ] Cache
- [ ] Consultas do banco
- [ ] Ãndices
- [ ] Teste com grande volume de transaÃ§Ãµes

### CenÃ¡rio-alvo de teste

O aplicativo deve continuar utilizÃ¡vel com histÃ³rico financeiro grande, incluindo milhares de transaÃ§Ãµes por usuÃ¡rio.

---

## 3.8 â€” Testes de carga e confiabilidade

- [ ] Teste de concorrÃªncia
- [ ] Teste de carga da API
- [ ] Teste de banco
- [ ] Teste de filas quando existirem
- [ ] Teste de integraÃ§Ã£o com IA
- [ ] Teste de sincronizaÃ§Ã£o
- [ ] Teste de falhas externas
- [ ] Teste de recuperaÃ§Ã£o

---

## 3.9 â€” DistribuiÃ§Ã£o

- [ ] ConfiguraÃ§Ã£o de release
- [ ] Assinatura do aplicativo
- [ ] Versionamento
- [ ] Build de produÃ§Ã£o
- [ ] Testes prÃ©-lanÃ§amento
- [ ] PreparaÃ§Ã£o da Play Console
- [ ] PolÃ­tica de privacidade publicada
- [ ] Termos publicados
- [ ] Ficha da loja
- [ ] Ãcone
- [ ] Screenshots
- [ ] DescriÃ§Ã£o
- [ ] EstratÃ©gia de atualizaÃ§Ã£o

---

# 5. Qualidade e processo de revisÃ£o

Toda microtarefa deverÃ¡ passar por este ciclo:

```text
â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”
â”‚ Definir microtarefa â”‚
â””â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”¬â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”˜
           â†“
â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”
â”‚ Implementar         â”‚
â””â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”¬â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”˜
           â†“
â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”
â”‚ Testar              â”‚
â””â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”¬â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”˜
           â†“
â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”
â”‚ Debug               â”‚
â””â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”¬â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”˜
           â†“
â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”
â”‚ Revisar arquitetura â”‚
â””â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”¬â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”˜
           â†“
â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”
â”‚ Refatorar           â”‚
â””â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”¬â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”˜
           â†“
â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”
â”‚ Integrar            â”‚
â””â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”¬â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”˜
           â†“
â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”
â”‚ Criar checkpoint    â”‚
â””â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”˜
```

## Definition of Done

Uma microtarefa sÃ³ Ã© considerada concluÃ­da quando:

- [ ] A funcionalidade atende ao objetivo
- [ ] O cÃ³digo estÃ¡ organizado
- [ ] O comportamento foi testado
- [ ] Os casos de erro relevantes foram tratados
- [ ] NÃ£o existem erros conhecidos crÃ­ticos
- [ ] NÃ£o houve regressÃ£o relevante
- [ ] A documentaÃ§Ã£o necessÃ¡ria foi atualizada
- [ ] O prÃ³ximo passo pode ser iniciado com a base atual

---

# 1.3 â€” DomÃ­nio financeiro

Objetivo: definir as entidades e invariantes financeiras no backend antes de criar tabelas, endpoints ou telas.

### Ordem desta microtarefa

```text
Transaction
  â†“
Account
  â†“
Category
  â†“
Transfer / Payment / Refund
  â†“
Installment / Recurrence
  â†“
Motor financeiro
```

### 1.3.1 â€” Transaction core

**Status:** CONCLUÃ�DA

- [x] Definir `TransactionId`, `AccountId` e `CategoryId`
- [x] Definir `Money` em unidade monetÃ¡ria mÃ­nima
- [x] Definir cÃ³digo de moeda
- [x] Definir direÃ§Ã£o `DEBIT/CREDIT`
- [x] Definir tipo de operaÃ§Ã£o
- [x] Definir status
- [x] Definir origem
- [x] Definir data do evento vs. timestamps de auditoria
- [x] Definir referÃªncia entre transaÃ§Ãµes relacionadas
- [x] Definir invariantes de validaÃ§Ã£o
- [x] Criar testes unitÃ¡rios
- [x] Validar compatibilidade com futuras parcelas e recorrÃªncias

### Invariantes iniciais

- Valor da transaÃ§Ã£o Ã© sempre positivo; a direÃ§Ã£o determina dÃ©bito/crÃ©dito.
- Valores monetÃ¡rios usam `Long` em unidade mÃ­nima, evitando `Double` para dinheiro.
- OperaÃ§Ãµes monetÃ¡rias exigem a mesma moeda.
- `INCOME` e `REFUND` sÃ£o crÃ©ditos.
- `EXPENSE`, `PURCHASE` e `PAYMENT` sÃ£o dÃ©bitos.
- `CANCELLATION` exige referÃªncia Ã  transaÃ§Ã£o relacionada.
- DescriÃ§Ã£o Ã© opcional, normalizada e limitada a 160 caracteres.
- Datas financeiras usam `LocalDate`; auditoria usa `Instant`.

**CritÃ©rio 1.3.1:** domÃ­nio compilando no projeto real, testes unitÃ¡rios passando no Gradle do projeto e invariantes documentadas. Validado com `clean test` + `build` e CI.

### PrÃ³ximas microtarefas 1.3

- [x] 1.3.2 â€” Account
- [x] 1.3.3 â€” Category
- [x] 1.3.4 â€” Transfer
- [x] 1.3.5 â€” Payment / Refund / Cancellation
- [x] 1.3.6 â€” Installment
- [x] 1.3.7 â€” Recurrence
- [x] 1.3.8 â€” RevisÃ£o de domÃ­nio

### 1.3.6 â€” Installment

**Status:** CONCLUÃ�DA

O agregado representa o cronograma de parcelamento sem lanÃ§ar automaticamente valores futuros no saldo.

- [x] DivisÃ£o exata do total em unidades monetÃ¡rias mÃ­nimas
- [x] DistribuiÃ§Ã£o determinÃ­stica do resto da divisÃ£o
- [x] Cronograma mensal com tratamento correto de fim de mÃªs
- [x] Limite de 2 a 360 parcelas
- [x] Estados do plano e das parcelas
- [x] Cancelamento protegido quando jÃ¡ existe parcela concluÃ­da
- [x] Testes unitÃ¡rios e build validados localmente

---

### 1.3.7 — Recurrence

**Status:** CONCLUÍDA

O agregado representa uma regra de recorrência e seu cronograma determinístico sem criar automaticamente lançamentos no saldo.

- [x] Frequências diária, semanal, mensal e anual
- [x] Intervalo configurável com limite de 1 a 365 unidades
- [x] Data inicial como âncora do cálculo
- [x] Limite opcional por data final e/ou quantidade de ocorrências
- [x] Tratamento de fim de mês sem deriva de datas
- [x] Tratamento de 29 de fevereiro sem deriva permanente
- [x] Estados ACTIVE, PAUSED e CANCELLED
- [x] Pausa, retomada e cancelamento protegidos por estado
- [x] Consultas de ocorrências por data e por número
- [x] Testes unitários e build validados localmente
---

### 1.3.8 — Revisão de domínio

**Status:** CONCLUÍDA

Revisão cruzada dos agregados financeiros, invariantes e fronteiras entre domínio e aplicação.

- [x] Unificar CategoryId em um único identificador fortemente tipado
- [x] Revisar Transaction, Account e Category
- [x] Revisar Transfer, Payment, Refund e Cancellation
- [x] Revisar Installment e Recurrence
- [x] Identificar invariantes que dependem de histórico persistido
- [x] Documentar responsabilidades de domínio vs. aplicação
- [x] Validar regressão da suíte de testes localmente

Documento: docs/domain/domain-review-1.3.8.md.
---

# 6. EstratÃ©gia de testes

## Testes unitÃ¡rios

Para:

- regras financeiras
- parsers
- cÃ¡lculos
- validaÃ§Ãµes
- Use Cases
- regras de domÃ­nio

## Testes de integraÃ§Ã£o

Para:

- API + banco
- autenticaÃ§Ã£o
- persistÃªncia
- sincronizaÃ§Ã£o
- integraÃ§Ãµes externas

## Testes instrumentados/UI

Para:

- fluxos crÃ­ticos
- login
- criaÃ§Ã£o de transaÃ§Ã£o
- navegaÃ§Ã£o
- fatura
- metas

## Testes manuais exploratÃ³rios

SerÃ£o usados para encontrar problemas de UX e cenÃ¡rios que testes automatizados nÃ£o cobrem bem.

---

# 7. Regras especiais do motor financeiro

O NEXA deve distinguir claramente:

```text
Saldo em conta
â‰ 
Limite do cartÃ£o
â‰ 
Limite disponÃ­vel
â‰ 
Valor da fatura
â‰ 
Valor efetivamente pago
```

TambÃ©m deverÃ¡ distinguir:

```text
Compra
TransferÃªncia
Pagamento
Estorno
Cancelamento
Parcela
RecorrÃªncia
```

Qualquer alteraÃ§Ã£o nessas regras exige novos testes de regressÃ£o.

---

# 8. Regras especiais da IA

A IA do NEXA deverÃ¡:

- trabalhar sobre contexto financeiro estruturado
- evitar inferÃªncias nÃ£o suportadas pelos dados
- declarar incerteza quando necessÃ¡rio
- nÃ£o alterar dados financeiros sem confirmaÃ§Ã£o explÃ­cita
- nÃ£o inventar valores, datas ou transaÃ§Ãµes
- respeitar permissÃµes do usuÃ¡rio
- receber somente os dados necessÃ¡rios para a tarefa

A IA serÃ¡ um **assistente de interpretaÃ§Ã£o**, nÃ£o a fonte primÃ¡ria dos dados financeiros.

---

# 9. Ordem oficial de execuÃ§Ã£o

A sequÃªncia oficial serÃ¡:

```text
PARTE 1
  â†“
1.1 Fechamento tÃ©cnico
  â†“
1.2 RepositÃ³rio + ambiente
  â†“
1.3 DomÃ­nio financeiro
  â†“
1.4 Banco + migrations
  â†“
1.5 AutenticaÃ§Ã£o
  â†“
1.6 API
  â†“
1.7 Motor financeiro
  â†“
1.8 IntegraÃ§Ã£o Android funcional
  â†“
1.9 Design System + refinamento visual
  â†“
1.10 MVP tÃ©cnico
  â†“
CHECKPOINT PARTE 1
  â†“
PARTE 2
  â†“
2.1 Registro rÃ¡pido
  â†“
2.2 Dashboard
  â†“
2.3 Contas
  â†“
2.4 CartÃµes/Faturas
  â†“
2.5 OrÃ§amentos
  â†“
2.6 Metas
  â†“
2.7 RecorrÃªncias
  â†“
2.8 HistÃ³rico
  â†“
2.9 IA
  â†“
2.10 NotificaÃ§Ãµes
  â†“
CHECKPOINT PARTE 2
  â†“
PARTE 3
  â†“
3.1 ProduÃ§Ã£o
  â†“
3.2 SincronizaÃ§Ã£o
  â†“
3.3 Open Finance
  â†“
3.4 SeguranÃ§a
  â†“
3.5 LGPD/Compliance
  â†“
3.6 Observabilidade
  â†“
3.7 Performance
  â†“
3.8 Carga/Confiabilidade
  â†“
3.9 DistribuiÃ§Ã£o
  â†“
CHECKPOINT FINAL
```

---

# 10. Regra de evoluÃ§Ã£o do documento

Este arquivo Ã© a **fonte de verdade do planejamento**.

Quando novas decisÃµes estruturais forem tomadas, o plano deverÃ¡ ser atualizado antes de iniciar uma nova etapa que dependa delas.

Durante a implementaÃ§Ã£o, o checklist serÃ¡ marcado progressivamente.

AlteraÃ§Ãµes no plano deverÃ£o preservar a coerÃªncia entre:

- arquitetura
- domÃ­nio
- banco
- API
- Android
- IA
- seguranÃ§a
- experiÃªncia do usuÃ¡rio

---

# 11. Resultado final esperado

Ao completar as trÃªs partes, o NEXA Finance deverÃ¡ possuir uma base preparada para ser um produto real:

```text
                 NEXA FINANCE
                       â”‚
        â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”¼â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”
        â”‚              â”‚              â”‚
     Android        Backend           IA
        â”‚              â”‚              â”‚
        â””â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”¼â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”˜
                       â”‚
                   PostgreSQL
                       â”‚
             â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”´â”€â”€â”€â”€â”€â”€â”€â”€â”€â”
             â”‚                   â”‚
        Open Finance        ServiÃ§os
          futuro             externos
```

A visÃ£o do produto Ã© simples:

> **O NEXA deve tornar a gestÃ£o financeira pessoal rÃ¡pida, clara e inteligente, sem sacrificar precisÃ£o, seguranÃ§a ou capacidade de escala.**

---

## PrÃ³xima execuÃ§Ã£o oficial

**PARTE 1 â†’ Microtarefa 1.3 â€” DomÃ­nio financeiro.**

ApÃ³s o fechamento tÃ©cnico e da infraestrutura local, o desenvolvimento passa a priorizar o domÃ­nio financeiro e suas invariantes antes do banco, API e acabamento visual.

---

# 1.2 â€” RepositÃ³rio e estrutura inicial

## Estado atual

- [x] Monorepo Git inicializado
- [x] `android/` e `backend/` como builds Gradle independentes
- [x] ConvenÃ§Ãµes de repositÃ³rio documentadas
- [x] `.editorconfig` e `.gitattributes`
- [x] ConfiguraÃ§Ã£o local via `.env.example`
- [x] Script para geraÃ§Ã£o do Gradle Wrapper
- [x] Gradle Wrapper Android validado em 9.5.0
- [x] Gradle Wrapper Backend validado em 9.5.0
- [x] JDK 17 validado
- [x] Android SDK / Platform 37.0 / Build Tools 36.0.0 validados
- [x] Build Android validado e APK debug gerado
- [x] Testes do Backend validados
- [x] Build limpo do Backend validado com JDK 17
- [x] WSL 2 habilitado e atualizado
- [x] Docker Desktop / Docker Engine validados
- [x] `docker compose config` validado
- [x] PostgreSQL em execuÃ§Ã£o saudÃ¡vel
- [x] RevisÃ£o final da infraestrutura local

### CorreÃ§Ã£o aplicada e resultado

O primeiro `postgres:18-alpine` iniciou em loop de reinÃ­cio porque o Compose montava o volume em `/var/lib/postgresql/data`. A partir do PostgreSQL 18, a imagem oficial passou a usar `/var/lib/postgresql` como ponto de volume e `PGDATA=/var/lib/postgresql/18/docker`. O Compose foi corrigido para seguir esse contrato e tambÃ©m passou a exigir `POSTGRES_PASSWORD` via `.env`, sem fallback de senha no arquivo versionado.

ApÃ³s a correÃ§Ã£o, uma inicializaÃ§Ã£o limpa foi validada: o container ficou `healthy`, `pg_isready` retornou `accepting connections`, e os logs confirmaram `database system is ready to accept connections`. O aviso de locale da imagem Alpine foi identificado como nÃ£o bloqueante para o ambiente atual.

### CritÃ©rio de conclusÃ£o 1.2

A microtarefa foi concluÃ­da apÃ³s verificar:

1. Android compilando com `assembleDebug`;
2. Backend passando em `clean build` e `test` com JDK 17;
3. Docker operacional;
4. `docker compose ... config` vÃ¡lido;
5. PostgreSQL `healthy` e respondendo a `pg_isready`;
6. `.env` fora do versionamento;
7. revisÃ£o final sem pendÃªncias bloqueadoras de ambiente, estrutura ou documentaÃ§Ã£o.

**Status: CONCLUÃDA âœ…**


# 12. Estado atual consolidado

## Checkpoint validado em 2026-10-03
- Ambiente Java 17 / Gradle 9.5.0 validado.
- Backend: testes e build passando.
- Android: testes/build e APK debug passando.
- Docker Compose: configuração validada.
- Git hook pre-push configurado em .githooks.
- Verificação completa: scripts/verify.ps1 passou com ExecutionPolicy Bypass.
- Domínio concluído: Transaction, Account, Category e Transfer.
- android/local.properties é local e está protegido pelo .gitignore.

## Próxima etapa
1.3.6 - Installment.


