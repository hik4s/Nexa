# NEXA Finance â€” Plano Mestre do Projeto

**VersÃ£o:** 1.0
**Status:** Documento-base do desenvolvimento

**Checkpoint atual:** Parte 2 - 2.10 Notificações e insights proativos concluído e validado; próximo passo 2.11 Gate de aprovação da Parte 2.
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

**Status:** EM PROGRESSO

Implementado e validado: cadastro, login, sessÃ£o persistida, logout, recuperaÃ§Ã£o de acesso, redefiniÃ§Ã£o de senha, perfil, autorizaÃ§Ã£o central e isolamento HTTP de contas financeiras entre usuÃ¡rios. O isolamento foi validado contra PostgreSQL real.

A etapa permanece aberta porque os demais recursos financeiros ainda nÃ£o possuem repositÃ³rios/endpoints HTTP; o isolamento desses recursos serÃ¡ aplicado antes de serem expostos pela API.

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

### Microtarefas

- [x] Cálculo de saldo
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

- [x] Cálculo de saldo
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




# Checkpoint 1.9 - 2026-10-03

Status: EM PROGRESSO.

Implementado e validado contra PostgreSQL real: transacoes manuais, calculo de saldo, listagem e consulta, cancelamento, validacao de moeda, contas ativas, autenticacao obrigatoria e isolamento por usuario. A persistencia tambem valida o escopo da categoria referenciada.

Pendentes: transferencias, cartoes/faturas/pagamento de fatura, parcelamento, estorno completo, recorrencia e testes correspondentes.

Este checkpoint e a referencia operacional atual da etapa 1.9. Itens pendentes nao devem ser marcados como concluidos sem implementacao, testes, debugging e validacao real.

# Checkpoint 1.9.1 - 2026-10-03

Transferências HTTP concluídas e validadas.

Implementado:
- POST /api/v1/transfers
- GET /api/v1/transfers
- GET /api/v1/transfers/{id}
- POST /api/v1/transfers/{id}/post
- POST /api/v1/transfers/{id}/cancel
- persistência atômica da transferência e das duas transações vinculadas;
- isolamento por usuário;
- validação de contas pertencentes ao usuário e ativas;
- validação de contas distintas;
- validação de mesma moeda;
- validação de valor positivo;
- publicação/cancelamento atômico das duas transações;
- saldo derivado corretamente somente de transações POSTED.

Validação:
- TransferTest: passou;
- TransferApiTest contra PostgreSQL real: passou;
- regressão completa com `clean test` passou;
- `scripts/verify.ps1` passou com `NEXA verification: PASS`;
- `git diff --check` passou.

Próximo vertical: cartões/faturas e pagamento de fatura.

# Checkpoint 1.9.2 - 2026-10-03

Cartoes, faturas e pagamento de fatura concluidos e validados.

Validacao:
- CardInvoiceApiTest contra PostgreSQL real;
- clean test: 123 testes, 0 falhas;
- health checks da API validados;
- git diff --check passou;
- scripts/verify.ps1 passou com NEXA verification: PASS;
- build backend, build Android e configuracao Docker Compose passaram.

Implementado:
- endpoints de cartoes para criacao, consulta, listagem e status;
- compras de cartao com limite disponivel e moeda validada;
- persistencia de card_id e invoice_id nas transacoes;
- geracao, fechamento, vencimento e pagamento parcial/integral de faturas;
- pagamento atomico em duas pernas entre conta de origem e passivo do cartao;
- isolamento por usuario e protecao contra fatura duplicada no mesmo periodo;
- correcao da persistencia dos metadados de cartao/fatura e do vinculo cruzado do pagamento.

Proximo vertical: reembolsos/cancelamentos completos e recorrencia, conforme dependencias do plano.

# Checkpoint 1.9.3 - 2026-10-03

Reembolsos e cancelamentos contábeis completos implementados e validados.

Implementado:
- POST /api/v1/transactions/{id}/refunds;
- POST /api/v1/transactions/{id}/cancellation;
- reembolso integral e parcial de despesas;
- reembolso de compras de cartão como DEBIT no passivo;
- reembolso permitido para cartão bloqueado/cancelado;
- limite acumulado de reembolso sem ultrapassar o valor original;
- cancellation como novo evento contábil, sem apagar o evento original;
- cancellation com direção oposta à transação original;
- proteção contra cancellation duplicada por regra de aplicação e índice único parcial;
- isolamento por usuário;
- proteção de operações de duas pernas: transferências continuam sob o agregado de transferência e pagamentos de fatura continuam sob o agregado da fatura.

Validação:
- RefundCancellationApiTest contra PostgreSQL real: 5 testes, 0 falhas, 0 skips;
- cenário de refund parcial acumulado validado;
- cenário de refund de compra de cartão com cartão BLOCKED validado;
- cenário de cancellation duplicada validado;
- isolamento entre usuários validado;
- compileKotlin passou;
- git diff --check deve passar no gate final.

Próximo vertical: recorrência, mantendo parcelamento HTTP como pendência separada.


# Checkpoint 1.9.4 - 2026-10-03

Recorrencias integradas ao motor financeiro e API validadas.

Implementado:
- V6 com recurrence_id e recurrence_occurrence_number em transactions;
- FK para recurrences e indice unico parcial por recorrencia/ocorrencia;
- repositorio JDBC de recorrencias;
- API de criacao, listagem, consulta, preview de ocorrencias, pause, resume e cancel;
- execucao de ocorrencia como Transaction SYSTEM;
- INCOME como CREDIT e EXPENSE como DEBIT;
- validacao de conta do proprietario, ACTIVE, moeda, categoria e bloqueio de conta CREDIT_CARD;
- execucao idempotente para a mesma ocorrencia;
- testes HTTP contra PostgreSQL real.

Validacao:
- RecurrenceApiTest: 4 testes, 0 falhas, 0 skips;
- suite completa backend clean test: BUILD SUCCESSFUL;
- backend build: BUILD SUCCESSFUL;
- Android clean test + assembleDebug: BUILD SUCCESSFUL;
- Docker Compose config: PASS;
- verify-database.ps1: PASS com migrations 1 a 6, 22 indices e 28 foreign keys;
- git diff --check: PASS.

A pendencia de parcelamento HTTP foi concluída.

# Checkpoint 1.9.5 - 2026-10-03

Parcelamento HTTP implementado e validado.

Implementado:
- domínio de parcelamento com divisão exata do valor em centavos;
- vencimentos mensais com tratamento de fim de mês;
- repositório JDBC com isolamento por usuário e persistência transacional;
- serviço de criação, consulta, conclusão de parcela e cancelamento;
- API autenticada de criação, listagem, consulta, conclusão e cancelamento;
- validação de moeda, limites, categoria e ownership;
- testes HTTP de ciclo de vida, isolamento, cancelamento e validações contra PostgreSQL real.

Validação:
- InstallmentApiTest: 2 testes, 0 falhas, 0 skips, 0 erros;
- schema PostgreSQL: PASS.

Próximo vertical: concluir a validação integrada final e avançar para o próximo item do mapa de dependências.

# Checkpoint 1.10.3 - 2026-10-04

MVP Android conectado à API e validado em runtime com instrumentação Compose no emulador Android 15 (API 35).

Implementado:
- cadastro e login contra a API real;
- cliente HTTP autenticado com Bearer token;
- criação e listagem de contas;
- saldo agregado das contas;
- criação de receitas e despesas manuais;
- histórico de transações;
- interface Compose mínima para o fluxo Parte 1;
- configuração de URL da API via BuildConfig;
- permissão de rede Android;
- liberação de HTTP apenas no build debug para comunicação local com `10.0.2.2:8080`;
- test tags semânticas para os principais controles do fluxo;
- teste instrumentado `FinanceE2ETest.register_login_account_and_transaction_flow`;
- formulário de lançamento mantido composto mesmo sem conta, com botão desabilitado até existir conta;
- atualização local de contas e feedback de sucesso após operações para evitar condições de corrida de refresh.

Validação final:
- emulator Android 15 / API 35 instalado e iniciado;
- ADB `emulator-5554` reconhecido como `device`;
- APK debug instalado com sucesso;
- MainActivity abriu em runtime;
- backend acessível pelo ambiente do emulador;
- bloqueio de cleartext HTTP no debug identificado e corrigido;
- ambiente do backend iniciado sem `.env` identificado e corrigido durante o teste manual;
- Android `testDebugUnitTest`: PASS;
- Android `assembleDebug`: PASS;
- Android `connectedDebugAndroidTest`: PASS — **1/1 testes, 0 falhas, 0 skips**;
- PostgreSQL real: PASS;
- backend `clean test build`: PASS;
- `git diff --check`: PASS.

Observação:
- O teste instrumentado emite apenas um warning de depreciação da API antiga `createAndroidComposeRule`; não há falha funcional. A migração para a API v2 fica registrada como melhoria técnica futura, sem bloquear 1.10.

Status:
- **1.10 CONCLUÍDA.**
- O bloqueio de runtime que existia em 1.10.2 foi fechado com teste instrumentado reproduzível.
- Próxima dependência: avançar para a próxima etapa da Parte 2 conforme o plano.

# Checkpoint 2.1 - 2026-10-04

Registro ultrarrápido da Parte 2 implementado e validado de ponta a ponta.

Implementado:
- endpoint autenticado de categorias de sistema;
- migration V7 com 10 categorias estáveis;
- teste de API de categorias com PostgreSQL real;
- entrada rápida com valor + descrição;
- parser de valores em BRL, tipo, categoria e descrição;
- seleção rápida de categoria;
- conta padrão persistida localmente;
- cartão padrão persistido localmente quando disponível;
- confirmação em poucos toques;
- atualização imediata do histórico usando a transação retornada pela API;
- teste instrumentado real cobrindo cadastro, login, conta, entrada natural, categoria, POST e histórico.

Validação:
- PostgreSQL: PASS; migrations 1-7 aplicadas;
- CategoryApiTest: PASS com PostgreSQL real;
- backend clean test build: PASS;
- Android testDebugUnitTest: PASS;
- Android assembleDebug: PASS;
- Android connectedDebugAndroidTest: PASS - 2/2 testes, 0 falhas, 0 skips;
- emulador Android 15 / API 35: PASS;
- persistência real confirmada no PostgreSQL com amount_minor=1890 e categoria Alimentação;
- git diff --check: PASS.

Observação:
- permanece apenas o warning de depreciação da API v1 createAndroidComposeRule; migração para Compose Test v2 fica como melhoria técnica futura e não bloqueia 2.1.

Status:
- 2.1 CONCLUÍDA.
- Próxima dependência: 2.2 Dashboard.
# Checkpoint 2.2 - 2026-10-04

Dashboard da Parte 2 implementado e validado de ponta a ponta.

Implementado:
- saldo total consolidado das contas;
- entradas do mês;
- despesas do mês;
- resultado do período;
- comparação de despesas com o mês anterior;
- gastos agrupados por categoria;
- evolução temporal semanal do período;
- gastos recorrentes ativos consumidos pela API existente;
- insight determinístico baseado nos gastos do período;
- estado explícito para orçamento ainda não configurado, sem simular dados;
- Dashboard separado em componente próprio;
- carregamento paralelo de contas, cartões, categorias, transações e recorrências;
- atualização imediata das métricas após entrada rápida;
- test tags para os blocos principais do Dashboard;
- E2E atualizado para validar o Dashboard real sem depender do histórico, reservado à etapa 2.8.

Validação:
- PostgreSQL real: PASS; migrations 1-7 aplicadas;
- backend clean test build: PASS;
- Android testDebugUnitTest: PASS;
- Android assembleDebug: PASS;
- Android connectedDebugAndroidTest: PASS - 2/2 testes, 0 falhas, 0 skips;
- teste isolado do fluxo completo: PASS;
- emulador Android 15 / API 35: PASS;
- persistência real via backend confirmada durante E2E;
- git diff --check: PASS.

Dependência explicitamente preservada:
- orçamento consumido permanece em estado vazio porque o domínio/migration de orçamento ainda não existe; sua implementação é responsabilidade da etapa 2.5. O Dashboard não inventa orçamento nem cria uma segunda fonte de verdade.

Observação:
- permanece o warning de depreciação de createAndroidComposeRule v1; migração para Compose Test v2 é melhoria futura e não bloqueia 2.2.

Status:
- 2.2 CONCLUÍDA.
- Próxima dependência: 2.3 Contas.

# Checkpoint 2.3 - 2026-10-04

Contas concluídas e validadas no aplicativo.

Implementado:
- listagem e detalhes de contas;
- histórico filtrado por conta com accountId real;
- tipos corrente, poupança e dinheiro em espécie;
- conta principal persistida por usuário com migration V8 e índice único;
- transferência entre contas integrada ao endpoint existente;
- fluxo Android com estados e test tags dos principais controles.

Validação:
- backend clean test build: PASS;
- Android testDebugUnitTest: PASS;
- Android assembleDebug: PASS;
- scripts/verify.ps1 completo: PASS;
- git diff --check: PASS.

# Checkpoint 2.4 - 2026-10-04

Cartões e faturas — camada Android integrada ao contrato existente.

Implementado:
- cadastro de cartão vinculado à conta de passivo;
- limite total e limite disponível;
- bandeira, últimos quatro dígitos, fechamento e vencimento;
- histórico de transações por cartão;
- geração de fatura para período corrente;
- listagem de faturas, total, pago, restante e vencimento;
- fechamento e pagamento de fatura;
- suporte a conta do tipo CREDIT_CARD no fluxo de contas.

Pendente deliberado:
- parcelas de cartão ainda não estão materializadas/associadas ao fluxo de compra; o domínio de parcelas existe separadamente. Esse item será integrado antes de marcar 2.4 como concluído.

Validação desta etapa:
- Android testDebugUnitTest: PASS;
- Android assembleDebug: PASS;
- git diff --check: PASS.


# Checkpoint 2.4.7 - 2026-10-04

Integração de compras parceladas no cartão concluída no backend.

Implementado:
- migration V9 vinculando cada transação a installmentId + installmentNumber;
- compra no cartão aceita installmentCount (1 mantém o comportamento à vista);
- criação atômica do plano, itens e transações das parcelas;
- cada parcela é uma transação POSTED/PENDING real do ledger, mantendo o valor total comprometido no limite quando postada;
- calendário de cobrança respeita fechamento/vencimento do cartão e distribui as parcelas pelos ciclos futuros;
- primeira parcela usa a data real da compra e as seguintes usam os fechamentos dos ciclos subsequentes;
- limite é revalidado sob lock da conta de passivo antes da persistência para evitar corrida de concorrência;
- resposta da compra expõe installmentId/installmentNumber;
- listagem de transações expõe metadados de parcelamento;
- teste de integração cobre 3 parcelas, divisão exata de centavos, vencimentos, limite, primeira fatura, pagamento e segunda fatura;
- cleanup dos testes atualizado para respeitar as novas FKs.

Validação:
- Flyway em PostgreSQL efêmero: PASS; 9 migrations aplicadas;
- teste crítico de integração: PASS;
- suíte backend completa em PostgreSQL efêmero: PASS;
- Android testDebugUnitTest: PASS;
- Android assembleDebug: PASS;
- git diff --check: PASS.

Limitação ambiental preservada:
- o PostgreSQL persistente local continua com credencial divergente da configuração atual do ambiente; nenhum volume ou dado existente foi removido ou alterado. A validação real foi feita em banco PostgreSQL efêmero isolado.

Próximo subitem:
- integrar a criação de compra parcelada ao fluxo visual Android, evitando deixar a capacidade existente apenas no contrato HTTP.


# Checkpoint 2.4.8 - 2026-10-04

Fluxo Android de compras parceladas integrado ao registro rápido.

Implementado:
- ApiClient.createCardPurchase agora envia installmentCount;
- registro rápido no modo Cartão oferece seleção de 1 a 12 parcelas;
- seleção de parcelas é enviada ao backend real, mantendo o mesmo fluxo de compra;
- resposta de compra preserva metadados de parcelamento no modelo Tx;
- seletor possui testTag quick_installments para validação automatizada;
- APK debug instalado com sucesso no emulador Android local;
- aplicação iniciou no emulador e a tela de autenticação foi validada via UIAutomator.

Validação:
- Android testDebugUnitTest: PASS;
- Android assembleDebug: PASS;
- APK install no emulator-5554: PASS;
- launch da aplicação: PASS;
- UIAutomator encontrou a tela NEXA Finance e controles de autenticação.

Limitação da validação visual:
- não foi realizado login no emulador porque não havia credencial de teste disponível; portanto o formulário autenticado de parcelamento não foi clicado manualmente no dispositivo. O caminho compilado e o contrato HTTP estão cobertos pelos testes existentes.

# Checkpoint 2.4.9 - 2026-10-04

Validação semântica do fluxo de parcelas concluída.

Implementado:
- QuickEntryCard aceita callback opcional de compra de cartão, preservando ApiClient como comportamento padrão de produção;
- valor inicial opcional permite testes determinísticos do formulário sem depender de teclado/IME;
- teste instrumentado cobre modo Cartão, seleção de 3 parcelas, envio de cardId, valor, descrição e installmentCount ao callback de compra;
- o contrato exercitado pelo callback corresponde aos quatro argumentos usados para chamar ApiClient.createCardPurchase.

Validação:
- Android testDebugUnitTest: PASS;
- Android assembleDebug: PASS;
- Android assembleDebugAndroidTest: PASS;
- teste instrumentado QuickEntryInstallmentTest no emulator-5554: PASS (1/1);
- APK principal e APK instrumentado instalados no emulador: PASS;
- git diff --check: PASS.

Conclusão 2.4:
- cadastro, limites, fechamento, vencimento, faturas, parcelas, pagamentos e histórico de cartões/faturas estão implementados;
- integração de parcelas backend + Android está validada;
- não há pendência técnica conhecida nesta vertical.

Próximo passo:
- iniciar 2.5 Orçamentos.


# Checkpoint 2.5.1 - 2026-10-04

Orçamentos implementados do backend ao Android.

Implementado:
- orçamento mensal com janela de vigência;
- orçamento geral ou vinculado a categoria de despesa;
- cálculo de consumo apenas sobre eventos POSTED relevantes, com REFUND abatendo consumo;
- progresso, valor consumido, restante e percentual;
- alertas OK, WARNING e EXCEEDED;
- limites personalizados por valor definido para cada orçamento;
- histórico de orçamentos com estados ACTIVE, PAUSED, COMPLETED e CANCELLED;
- operações de atualizar, pausar, retomar, concluir e cancelar;
- migration V10 para persistência de orçamentos;
- API e painel Android integrados ao dashboard;
- entrada monetária do painel convertida em centavos com BigDecimal, evitando arredondamento via Double;
- teste instrumentado do BudgetPanel;
- E2E Android endurecido para rolagem semântica em LazyColumn, sem depender de índices fixos.

Validação:
- Flyway V1→V10 em PostgreSQL 18.6 efêmero: PASS;
- JdbcBudgetRepositoryIntegrationTest: PASS;
- suíte backend completa: 142/142 PASS;
- BudgetPanelTest no emulator-5554: PASS (1/1);
- QuickEntryInstallmentTest no emulator-5554: PASS (1/1);
- FinanceE2ETest com Ktor real + PostgreSQL efêmero: PASS (2/2);
- Android assembleDebug: PASS;
- git diff --check: PASS.

Observação de ambiente:
- a suíte Android completa executada sem backend ativo apresentou falhas de E2E por ausência do serviço em 10.0.2.2:8080; com Ktor + PostgreSQL efêmero ativos, os dois fluxos FinanceE2ETest passaram 2/2.
- nenhum volume ou dado do PostgreSQL persistente local foi removido ou alterado.

Conclusão 2.5:
- todas as microtarefas de orçamentos estão implementadas e validadas;
- não há pendência técnica conhecida nesta vertical.

Próximo passo:
- iniciar 2.6 Metas financeiras.


# Checkpoint 2.6.1 - 2026-10-04

Metas financeiras implementadas e validadas do domínio ao Android.

Implementado:
- criação de meta com nome, valor-alvo, moeda, prazo e descrição;
- valor acumulado e progresso percentual;
- contribuições persistidas individualmente com histórico;
- aporte transacional com lock da meta, impedindo ultrapassar o alvo em concorrência;
- estados ACTIVE, PAUSED, ACHIEVED e CANCELLED;
- pausar, retomar e cancelar meta;
- projeção baseada no ritmo médio mensal, com estados ON_TRACK, AT_RISK, NO_CONTRIBUTION, NO_DEADLINE, ACHIEVED e PAUSED;
- migration V11 para metas e contribuições;
- API autenticada de criação, listagem, detalhe, aporte e ciclo de vida;
- integração Android no dashboard;
- criação de meta, aporte, progresso, prazo, histórico e projeção no painel;
- conversão monetária do formulário para centavos com BigDecimal;
- testes de domínio, serviço/projeção, integração PostgreSQL e API;
- E2E Android autenticado cobrindo criação da meta e aporte até atingir o alvo.

Validação:
- Flyway V1→V11 em PostgreSQL 18.6 efêmero: PASS;
- GoalTest: PASS;
- GoalProjectionTest: PASS;
- JdbcGoalRepositoryIntegrationTest: PASS;
- ApplicationTest com fluxo de metas: PASS;
- suíte backend completa: PASS;
- Android testDebugUnitTest: PASS;
- Android assembleDebug: PASS;
- GoalPanelTest no emulator-5554: PASS (1/1);
- FinanceE2ETest com Ktor real + PostgreSQL efêmero: PASS (2/2);
- suíte Android instrumentada completa: PASS (5/5, 0 falhas);
- git diff --check: PASS.

Observação de ambiente:
- a validação usou PostgreSQL efêmero e Ktor local; o volume PostgreSQL persistente existente não foi removido nem alterado.
- o CI do commit 972f0a5 (Orçamentos) concluiu com sucesso no GitHub Actions.

Conclusão 2.6:
- todas as microtarefas planejadas de metas financeiras estão implementadas e validadas;
- não há pendência técnica conhecida nesta vertical.

# Checkpoint 2.7.1 - 2026-10-04

Recorrências implementadas e validadas do domínio ao Android.

Implementado:
- criação e edição de recorrências;
- frequências diária, semanal, mensal e anual com intervalo configurável;
- data inicial, final e limite de ocorrências;
- pausar, retomar e encerrar recorrência;
- geração determinística de ocorrências sem criar lançamentos automaticamente;
- resumo de próxima ocorrência, próximas cobranças e nível de alerta;
- execução manual de ocorrência como transação real do ledger;
- execução idempotente por recorrência + número da ocorrência;
- bloqueio de execução em contas arquivadas ou cartões de crédito;
- detecção de possíveis recorrências a partir de lançamentos POSTED;
- isolamento por usuário, conta, categoria e moeda;
- API autenticada de criação, edição, listagem, detalhe, ocorrências, ciclo de vida, execução e candidatos;
- integração Android no dashboard com formulário, edição, pausa/retomada, encerramento e sugestões detectadas;
- testes de domínio, insights, API e Android instrumentado.

Validação:
- PostgreSQL 18.6 com Flyway V1→V11: PASS;
- RecurrenceInsightsTest: PASS;
- RecurrenceApiTest: PASS (6/6);
- suíte backend completa: PASS;
- Android testDebugUnitTest: PASS;
- Android assembleDebug: PASS;
- RecurrencePanelTest no emulator-5554: PASS (1/1);
- git diff --check: PASS.

Correção relevante:
- corrigido filtro opcional de conta em JdbcTransactionRepository.findByOwner, tipando explicitamente UUID nulo para PostgreSQL.

Conclusão 2.7:
- todas as microtarefas planejadas de recorrências estão implementadas e validadas;
- não há pendência técnica conhecida nesta vertical.

Próximo passo:
- iniciar 2.8 Busca, filtros e histórico.

# Checkpoint 2.8.1 - 2026-10-04

Busca, filtros e histórico implementados e validados no Android.

Implementado:
- busca textual por descrição;
- filtros por período, categoria, conta, cartão e tipo;
- ordenação por data e valor;
- detalhamento expandível da transação;
- exibição de conta, categoria, cartão, fatura e parcela quando disponíveis;
- limpeza de todos os filtros;
- limite visual de 50 resultados para manter o painel responsivo;
- integração do painel ao Dashboard real usando as transações já carregadas pela API.

Validação:
- Android testDebugUnitTest: PASS;
- Android assembleDebug: PASS;
- TransactionHistoryPanelTest no emulator-5554: PASS (1/1);
- git diff --check: PASS.

Observação de ambiente:
- o teste instrumentado foi executado no Android 15 / API 35 real disponível na máquina;
- nenhuma alteração destrutiva foi feita no PostgreSQL persistente local.

Conclusão 2.8:
- todas as microtarefas planejadas de busca, filtros e histórico estão implementadas e validadas;
- não há pendência técnica conhecida nesta vertical.

Próximo passo:
- iniciar 2.9 IA Financeira.

# Checkpoint 2.9.1 - 2026-10-04

IA Financeira implementada como uma camada segura e desacoplada de fornecedor.

Implementado:
- interface AiProvider desacoplada do fornecedor/modelo;
- RuleBasedAiProvider como provider inicial determinístico e testável;
- Context Builder com allowlist de dados financeiros agregados;
- análise mensal de entradas, despesas e resultado;
- análise de categorias e concentração de gastos;
- análise de orçamentos e metas;
- detecção de tendência mensal de despesas;
- perguntas financeiras com respostas somente quando há dados suficientes;
- proteção contra respostas inventadas por meio de respostas estruturadas + sourceFactIds;
- origem dos números registrada em cada insight/resposta;
- limite de 20 consultas de IA por usuário/dia, persistido e atômico;
- migration V12 para controle de uso;
- API autenticada de análise, perguntas e uso diário;
- painel Android de IA com insights, fontes e perguntas;
- nenhum dado de descrição/transação pessoal é enviado ao provider atual.

Validação:
- RuleBasedAiProviderTest: PASS (3/3);
- JdbcAiUsageRepositoryIntegrationTest: PASS;
- AiContextBuilderIntegrationTest: PASS;
- Flyway V1→V12 em PostgreSQL 18.6: PASS;
- suíte backend completa: PASS (142/142);
- Android testDebugUnitTest: PASS;
- Android assembleDebug: PASS;
- AiPanelTest no emulator-5554: PASS (1/1);
- git diff --check: PASS.

Decisão arquitetural importante:
- o NEXA ainda não chama um LLM externo nesta etapa porque nenhuma credencial de provider está configurada na máquina e não seria correto enviar dados financeiros para um serviço externo sem uma configuração explícita.
- quando um provider externo for escolhido, ele deverá implementar AiProvider e receber apenas o AiFinancialContext permitido.

Conclusão 2.9:
- todas as microtarefas planejadas da camada segura de IA estão implementadas e validadas;
- não há pendência técnica conhecida nesta vertical;
- integração com um LLM externo fica como implementação de provider, sem alteração do contrato ou da UI.

Próximo passo:
- iniciar 2.11 Gate de aprovação da Parte 2.

# Checkpoint 2.10.1 - 2026-10-04

Notificações e insights proativos implementados e validados do backend ao Android.

Implementado:
- alertas de orçamento com deduplicação por usuário;
- alertas de metas e prazo próximo;
- avisos de recorrências próximas;
- avisos de faturas vencidas/próximas do vencimento;
- detecção de alteração significativa de gastos;
- insight periódico baseado exclusivamente no contexto financeiro interno;
- API autenticada para listar alertas e marcar como lidos;
- persistência V13 com isolamento por usuário e chave de deduplicação;
- painel Android com filtro de não lidos;
- configuração local para habilitar notificações do sistema;
- publicação de notificações nativas sem enviar dados financeiros a terceiros.

Validação:
- PostgreSQL 18.6 efêmero: PASS;
- Flyway V1→V13: PASS;
- teste de deduplicação do repositório: PASS;
- backend BUILD SUCCESSFUL;
- Android testDebugUnitTest: PASS;
- Android assembleDebug: PASS;
- git diff --check: PASS.

Observação:
- permanecem apenas os gates transversais da Parte 2; a vertical 2.10 não possui pendência técnica conhecida.