# NEXA Finance — Plano Mestre do Projeto

**Versão:** 1.0
**Status:** Documento-base do desenvolvimento

**Checkpoint atual:** 2026-10-07 — base de domínio, banco, infraestrutura, confiabilidade e Design System já avançados; Run #169 validado 100% verde. O próximo foco oficial é produzir evidência operacional reproduzível por meio do drill de falha externa + recuperação, sem deixar pendências de confiabilidade para trás.
**Plataforma inicial:**** Android
**Objetivo:** Construir um aplicativo de finanças pessoais sólido, seguro, escalável e preparado para distribuição futura.

---

## 0. Visão do produto

O **NEXA Finance** será um aplicativo de gestão financeira pessoal focado em três ideias centrais:

1. **Registrar gastos em poucos segundos.**
2. **Transformar registros em uma visão financeira clara.**
3. **Usar IA para interpretar a situação financeira e gerar insights úteis.**

A integração bancária via **Open Finance** será tratada como uma evolução posterior. A arquitetura já deverá nascer preparada para essa expansão, mas o MVP não dependerá dela.

### Princípio de produto

> O usuário não deve preencher manualmente informações que o NEXA consegue inferir com segurança.

Exemplo:

`18,90 almoço`

poderá resultar automaticamente em:

- Valor: R$ 18,90
- Tipo: despesa
- Categoria: Alimentação
- Descrição: Almoço
- Data: hoje
- Conta/cartão: padrão selecionado

Meta de experiência: **registro comum em menos de 20 segundos**.

---

# 1. Decisões arquiteturais

## 1.1 Stack tecnológica base

### Android

- Kotlin
- Jetpack Compose
- Material 3, adaptado ao Design System próprio do NEXA
- MVVM + princípios de Clean Architecture
- Coroutines
- Flow
- Navigation Compose
- Testes unitários e instrumentados

### Backend

- Kotlin
- Ktor
- API REST
- Arquitetura modular por domínio
- Validação de entrada
- Autenticação e autorização

### Banco de dados

- PostgreSQL
- Migrações versionadas
- Integridade referencial
- Índices definidos conforme os padrões de consulta reais

### Infraestrutura

- Docker para desenvolvimento e ambientes reproduzíveis
- Variáveis de ambiente/secrets fora do código
- CI/CD será incorporado progressivamente

### IA

- Camada de integração desacoplada do fornecedor/modelo
- Context Builder próprio
- Regras de segurança e filtragem de dados antes da chamada ao modelo
- Possibilidade de trocar o modelo sem reescrever o aplicativo

### Integrações futuras

- Open Finance
- Notificações
- Analytics/observabilidade
- Serviços de produção

---

# 2. Arquitetura geral

```text
                         NEXA Finance
                              │
             ┌────────────────┴────────────────┐
             │                                 │
       Android App                         Backend API
             │                                 │
    ┌────────┼────────┐                ┌───────┼────────┐
    │        │        │                │       │        │
Presentation Domain  Data           Auth   Finance    AI
    │        │        │                │       │        │
    └────────┴────────┘                └───────┼────────┘
                                              │
                                         PostgreSQL
                                              │
                                      Integrações futuras
                                      ├── Open Finance
                                      └── Serviços externos
```

## 2.1 Regra de separação

A interface não deve conter regras financeiras complexas.

Fluxo preferencial:

```text
UI
 ↓
ViewModel / State
 ↓
Use Case
 ↓
Domain
 ↓
Repository
 ↓
Data Source / API / Database
```

No backend:

```text
HTTP / Controller
 ↓
Application / Use Case
 ↓
Domain
 ↓
Repository
 ↓
PostgreSQL / External Services
```

---

# 3. Identidade visual do NEXA Finance

## 3.1 Conceito

A identidade visual deve transmitir:

- sofisticação
- precisão
- confiança
- discrição
- inteligência
- sensação premium
- simplicidade
- organização

A inspiração estética vem de marcas conhecidas pela **contenção visual, uso de espaço, tipografia forte e materiais de aparência premium**, mas o NEXA deverá possuir identidade própria e não copiar elementos proprietários de nenhuma marca.

## 3.2 Direção estética

**Minimalismo premium.**

O aplicativo deve evitar excesso de gradientes, sombras fortes, bordas desnecessárias, ícones decorativos e informações simultâneas demais.

A interface deverá privilegiar:

- bastante espaço em branco/negativo
- hierarquia tipográfica clara
- poucos elementos por tela
- microanimações discretas
- componentes com acabamento refinado
- contraste elevado
- feedback visual sem poluição

## 3.3 Paleta principal

### Obsidian

`#0B0F0D`

Uso:
- fundos escuros
- cabeçalhos
- navegação
- áreas premium

### Ivory

`#FAFAF7`

Uso:
- fundo claro principal
- superfícies
- contraste com Obsidian

### Burgundy

`#7A1F2E`

Uso:
- marca
- destaques
- ações especiais
- estados premium

### Deep Green

`#0F6B4D`

Uso:
- informações positivas
- saldo saudável
- metas
- crescimento

### Soft Gray

`#94A3B8`

Uso:
- textos secundários
- descrições
- separadores discretos

### Champagne Accent

`#E8DCC6`

Uso extremamente moderado:
- pequenos detalhes premium
- indicadores especiais
- momentos de destaque

## 3.4 Regra de uso das cores

A maior parte da interface deverá permanecer em **Obsidian + Ivory + tons neutros**.

Burgundy e Deep Green funcionam como cores de identidade e semântica. Champagne será raro.

Evitar transformar toda a interface em uma composição multicolorida.

## 3.5 Tipografia

Prioridade:

1. legibilidade
2. hierarquia
3. personalidade discreta

Base recomendada:

- fonte sans-serif moderna
- pesos bem definidos
- números financeiros com destaque
- títulos relativamente grandes e limpos
- textos secundários menores, porém legíveis

O sistema tipográfico deverá ser centralizado em tokens do Design System.

## 3.6 Forma dos componentes

- cantos moderadamente arredondados
- bordas finas quando necessárias
- sombras quase imperceptíveis
- poucos níveis de elevação
- cards usados somente quando agregarem organização
- botões simples e sólidos
- ícones lineares e consistentes

## 3.7 Sensação desejada

A tela deverá passar a impressão de:

> “Isto é organizado, preciso e caro, sem precisar parecer chamativo.”

---

# 4. Princípios permanentes do desenvolvimento

Estas regras valem para todo o projeto.

## 4.1 Microtarefas isoladas

Cada tarefa deve possuir:

- objetivo claro
- entradas
- implementação
- testes
- revisão
- integração
- critério de conclusão

## 4.2 Não acumular dívida técnica deliberadamente

Evitar:

- código duplicado sem justificativa
- regras de negócio espalhadas
- valores mágicos
- dependências desnecessárias
- arquivos gigantes
- telas responsáveis por lógica de domínio
- soluções provisórias sem registro

## 4.3 Debug contínuo

Todo erro encontrado deve ser tratado na origem sempre que possível.

Não utilizar “depois consertamos” como método de desenvolvimento.

## 4.4 Regressão

Toda alteração significativa deve verificar se funcionalidades anteriores continuam funcionando.

## 4.5 Segurança desde o início

Dados financeiros são sensíveis. Nenhuma etapa deverá depender de práticas inseguras apenas por ser “MVP”.

## 4.6 Preparação para escala

A arquitetura deve suportar a evolução de:

```text
1 usuário
→ 100 usuários
→ 10.000 usuários
→ escala maior
```

sem exigir reconstrução completa do produto.

---

# PARTE 1 — FUNDAÇÃO

Objetivo: construir uma base técnica sólida antes de acelerar o desenvolvimento de funcionalidades.

---

## 1.1 — Fechamento técnico

### Microtarefas

- [x] Confirmar Kotlin como linguagem principal do Android
- [x] Confirmar Kotlin + Ktor para backend
- [x] Confirmar Jetpack Compose
- [x] Confirmar MVVM + Clean Architecture
- [x] Confirmar PostgreSQL
- [x] Definir estratégia de migrations
- [x] Definir estrutura de módulos
- [x] Definir estratégia de ambientes
- [x] Definir política de dependências
- [x] Definir padrão de versionamento
- [x] Definir convenções de código

### Critério de conclusão

Todas as escolhas devem estar documentadas e ser suficientes para iniciar a implementação sem decisões estruturais pendentes.

---

## 1.2 — Repositório e estrutura inicial

### Microtarefas

- [ ] Criar repositório
- [ ] Criar estrutura Android
- [ ] Criar estrutura backend
- [ ] Criar documentação inicial
- [ ] Configurar Git
- [ ] Criar `.gitignore`
- [ ] Separar configurações locais de configurações versionadas
- [ ] Criar estratégia de branches

### Critério de conclusão

O projeto deve clonar e iniciar de forma reprodutível em ambiente limpo.

---

## 1.3 — Ambiente de desenvolvimento

### Microtarefas

- [ ] Configurar JDK/SDK necessários
- [ ] Configurar Android Studio
- [ ] Configurar Gradle
- [ ] Configurar backend
- [ ] Configurar Docker
- [ ] Subir PostgreSQL local
- [ ] Criar configuração de ambiente local
- [ ] Documentar setup

### Critério de conclusão

Outra máquina deve conseguir reproduzir o ambiente seguindo o README sem dependências ocultas.

---

## 1.4 — Design System

**Status:** CONCLUÍDO — identidade visual restaurada e alinhada à referência aprovada em 2026-10-07.

Microtarefas concluídas: tokens de cor, tipografia, espaçamento, raio, elevação, componentes base, estados de botão, campos de entrada, cards, valores monetários, feedback/loading/error states e modos claro/escuro. Validação: assembleDebug + testDebugUnitTest + lintDebug.


### Microtarefas

- [x] Criar tokens de cor
- [x] Criar tokens tipográficos
- [x] Criar tokens de espaçamento
- [x] Criar tokens de raio
- [x] Criar tokens de elevação
- [x] Criar componentes base
- [x] Criar estados de botão
- [x] Criar campos de entrada
- [x] Criar cards
- [x] Criar componentes de valor monetário
- [x] Criar feedback/loading/error states
- [x] Criar modo claro
- [x] Criar modo escuro
- [x] Validar acessibilidade básica

### Critério de conclusão

Novas telas devem conseguir utilizar o Design System sem criar estilos ad hoc.

---

## 1.5 — Banco de dados e migrações

**Status:** CONCLUÍDO

### Microtarefas

- [x] Criar PostgreSQL local
- [x] Definir convenções de tabelas
- [x] Criar migrations
- [x] Definir chaves primárias
- [x] Definir foreign keys
- [x] Definir índices iniciais
- [x] Definir timestamps
- [x] Definir estratégia de auditoria quando necessária
- [x] Testar criação do banco do zero
- [x] Testar rollback quando aplicável

### Critério de conclusão

O banco é reproduzível por migrations, possui integridade referencial adequada e sua recriação local foi validada do volume vazio até a aplicação das migrations V1 e V2.

A reversão adotada neste estágio é por nova migration; para o ambiente local descartável, a reconstrução do volume foi testada.

---

## 1.6 — Modelo de domínio financeiro

**Status:** EM ANDAMENTO

Revisão 1.6: User, Account, Transaction, Category, PaymentMethodType, Transfer, Payment, Refund, Cancellation, Installment, Budget, Goal e Recurrence estão modelados e testados. Card e Invoice aguardam a decisão contábil do ledger de cartão.

### Entidades principais

- [x] User
- [x] Account
- [x] Transaction
- [x] Category
- [x] PaymentMethod
- [ ] Card
- [ ] Invoice
- [x] Installment
- [x] Budget
- [x] Goal
- [x] RecurringTransaction

### Microtarefas

- [ ] Definir campos
- [ ] Definir relacionamentos
- [ ] Definir regras de estado
- [ ] Definir tipos monetários
- [ ] Definir datas e timezone
- [ ] Definir regras para valores positivos/negativos
- [ ] Definir transferências
- [ ] Definir estornos
- [ ] Definir cancelamentos

### Critério de conclusão

O domínio deve representar corretamente as operações financeiras previstas sem depender da interface.

---

## 1.7 — Autenticação e usuários

### Microtarefas

- [ ] Criar cadastro
- [ ] Criar login
- [ ] Criar sessão
- [ ] Criar logout
- [ ] Criar recuperação de acesso
- [ ] Criar perfil básico
- [ ] Implementar autorização por usuário
- [ ] Testar isolamento de dados entre usuários

### Critério de conclusão

Um usuário não pode acessar dados pertencentes a outro usuário.

---

## 1.8 — API base

### Microtarefas

- [ ] Criar servidor Ktor
- [ ] Configurar rotas
- [ ] Criar serialização
- [ ] Criar validação de requests
- [ ] Criar tratamento central de erros
- [ ] Criar respostas padronizadas
- [ ] Criar autenticação
- [ ] Criar documentação inicial da API
- [ ] Criar health check

### Critério de conclusão

A API deve responder de maneira consistente e previsível a requests válidos e inválidos.

---

## 1.9 — Motor financeiro

### Microtarefas

- [ ] Cálculo de saldo
- [ ] Registro de receita
- [ ] Registro de despesa
- [ ] Transferência entre contas
- [ ] Compra no cartão
- [ ] Fatura
- [ ] Pagamento de fatura
- [ ] Parcelamento
- [ ] Estorno
- [ ] Cancelamento
- [ ] Recorrência

### Testes obrigatórios

- [ ] Valores positivos
- [ ] Valores negativos
- [ ] Zero
- [ ] Casas decimais
- [ ] Datas diferentes
- [ ] Parcelas
- [ ] Transferências
- [ ] Estornos
- [ ] Cenários combinados

### Critério de conclusão

O motor deve produzir resultados determinísticos e testados para os principais fluxos financeiros.

---

## 1.10 — MVP técnico

O final da Parte 1 deverá permitir:

```text
Abrir NEXA
↓
Criar conta / Login
↓
Criar conta financeira
↓
Adicionar receita
↓
Adicionar despesa
↓
Atualizar saldo
↓
Consultar histórico
```

### Gate de aprovação da Parte 1

- [ ] Build Android funcionando
- [ ] Backend funcionando
- [ ] Banco reproduzível
- [ ] Autenticação funcionando
- [ ] CRUD essencial funcionando
- [ ] Motor financeiro testado
- [ ] Logs básicos funcionando
- [ ] Documentação atualizada
- [ ] Nenhum bug crítico aberto

---

# PARTE 2 — PRODUTO

Objetivo: transformar a fundação em uma experiência financeira completa e agradável.

---

## 2.1 — Registro ultrarrápido

- [ ] Fluxo de criação rápida
- [ ] Entrada numérica
- [ ] Descrição curta
- [ ] Seleção rápida de categoria
- [ ] Conta padrão
- [ ] Cartão padrão
- [ ] Confirmação em poucos toques
- [ ] Entrada em linguagem natural
- [ ] Parser de valores
- [ ] Parser de categoria
- [ ] Parser de descrição
- [ ] Correção rápida antes da confirmação
- [ ] Teste de tempo de execução do fluxo

### Meta

Gastos comuns devem ser registrados em menos de 20 segundos.

---

## 2.2 — Dashboard

- [ ] Saldo
- [ ] Entradas
- [ ] Despesas
- [ ] Resultado do período
- [ ] Gráfico por categoria
- [ ] Evolução temporal
- [ ] Comparação com períodos anteriores
- [ ] Gastos recorrentes
- [ ] Orçamento consumido
- [ ] Insights destacados

---

## 2.3 — Contas

- [ ] Listagem
- [ ] Detalhes
- [ ] Saldo
- [ ] Histórico
- [ ] Transferências
- [ ] Conta principal
- [ ] Contas secundárias
- [ ] Dinheiro em espécie

---

## 2.4 — Cartões e faturas

- [ ] Cadastro de cartão
- [ ] Limite
- [ ] Limite disponível
- [ ] Fechamento
- [ ] Vencimento
- [ ] Fatura atual
- [ ] Próximas faturas
- [ ] Parcelas
- [ ] Pagamento
- [ ] Histórico

---

## 2.5 — Orçamentos

- [ ] Orçamento mensal
- [ ] Orçamento por categoria
- [ ] Barra de progresso
- [ ] Alertas
- [ ] Limites personalizados
- [ ] Histórico de orçamento

---

## 2.6 — Metas financeiras

- [ ] Criar meta
- [ ] Valor-alvo
- [ ] Valor acumulado
- [ ] Prazo
- [ ] Progresso
- [ ] Contribuições
- [ ] Projeção

---

## 2.7 — Recorrências

- [ ] Criar recorrência
- [ ] Editar recorrência
- [ ] Pausar recorrência
- [ ] Encerrar recorrência
- [ ] Detectar possíveis recorrências
- [ ] Próximas cobranças
- [ ] Alertas

---

## 2.8 — Busca, filtros e histórico

- [ ] Busca textual
- [ ] Filtro por período
- [ ] Filtro por categoria
- [ ] Filtro por conta
- [ ] Filtro por cartão
- [ ] Filtro por tipo
- [ ] Ordenação
- [ ] Detalhamento da transação

---

## 2.9 — IA Financeira

### Arquitetura

```text
Dados financeiros
↓
Motor de análise
↓
Context Builder
↓
Modelo de IA
↓
Resposta estruturada
↓
NEXA App
```

### Microtarefas

- [ ] Criar interface de provider de IA
- [ ] Criar camada de contexto
- [ ] Definir quais dados podem ser enviados
- [ ] Criar análise mensal
- [ ] Criar explicação de gastos
- [ ] Criar detecção de padrões
- [ ] Criar insights
- [ ] Criar perguntas financeiras
- [ ] Criar análise de metas
- [ ] Criar análise de orçamento
- [ ] Criar proteção contra respostas inventadas
- [ ] Registrar origem dos números utilizados
- [ ] Criar limites de uso

### Princípio

A IA não deve inventar números. Toda afirmação quantitativa precisa ter origem nos dados disponíveis no NEXA.

---

## 2.10 — Notificações e insights proativos

- [ ] Alertas de orçamento
- [ ] Despesas recorrentes próximas
- [ ] Fatura próxima do vencimento
- [ ] Meta
- [ ] Alteração significativa de gasto
- [ ] Insight periódico
- [ ] Configurações de notificações

---

## 2.11 — Gate de aprovação da Parte 2

- [ ] Registro rápido validado
- [ ] Dashboard consistente
- [ ] Contas funcionando
- [ ] Cartões funcionando
- [ ] Faturas funcionando
- [ ] Orçamentos funcionando
- [ ] Metas funcionando
- [ ] Recorrências funcionando
- [ ] IA integrada com segurança
- [ ] Testes de regressão executados
- [ ] Nenhum bug crítico aberto

---

# PARTE 3 — ESCALA

Objetivo: preparar o NEXA para usuários reais, crescimento, integrações bancárias e distribuição.

---

## 3.1 — Backend de produção

- [ ] Ambiente de produção
- [ ] Configuração segura de secrets
- [ ] Banco de produção
- [ ] Migrations de produção
- [ ] HTTPS
- [ ] Rate limiting
- [ ] Versionamento de API
- [ ] Políticas de timeout
- [ ] Retries controlados
- [ ] Tratamento de falhas
- [ ] Backups
- [ ] Recuperação de desastre

---

## 3.2 — Sincronização

- [ ] Sincronização de dados
- [ ] Controle de versão
- [ ] Estratégia offline
- [ ] Retry
- [ ] Estados de conflito
- [ ] Idempotência
- [ ] Consistência entre dispositivos

---

## 3.3 — Open Finance

A integração bancária será adicionada somente quando a base financeira e a infraestrutura estiverem maduras.

### Microtarefas

- [ ] Levantar requisitos regulatórios aplicáveis
- [ ] Mapear fornecedores/participantes de integração
- [ ] Definir arquitetura de integração
- [ ] Implementar consentimento
- [ ] Implementar conexão
- [ ] Importar contas
- [ ] Importar transações
- [ ] Importar cartões quando suportado
- [ ] Categorizar transações
- [ ] Criar conciliação
- [ ] Tratar duplicidades
- [ ] Tratar revogação de consentimento
- [ ] Monitorar sincronização

---

## 3.4 — Segurança avançada

- [ ] Proteção de credenciais
- [ ] Proteção de tokens
- [ ] Armazenamento seguro no dispositivo
- [ ] Criptografia quando necessária
- [ ] Autorização por recurso
- [ ] Proteção de endpoints
- [ ] Detecção de abuso
- [ ] Auditoria
- [ ] Política de retenção de dados
- [ ] Revisão de exposição de dados da IA

---

## 3.5 — LGPD e compliance

- [ ] Mapear dados pessoais
- [ ] Mapear dados sensíveis/financeiros tratados
- [ ] Definir finalidade de tratamento
- [ ] Política de privacidade
- [ ] Termos de uso
- [ ] Gestão de consentimentos quando aplicável
- [ ] Exportação de dados
- [ ] Exclusão de conta/dados conforme requisitos aplicáveis
- [ ] Retenção e descarte

---

## 3.6 — Observabilidade

- [ ] Logs estruturados
- [ ] Métricas
- [ ] Crash reporting
- [ ] Tracing quando necessário
- [ ] Alertas
- [ ] Monitoramento de API
- [ ] Monitoramento do banco
- [ ] Monitoramento de integrações externas

---

## 3.7 — Performance

- [ ] Tempo de inicialização
- [ ] Tempo de abertura de telas
- [ ] Uso de memória
- [ ] Uso de bateria
- [ ] Uso de rede
- [ ] Paginação
- [ ] Cache
- [ ] Consultas do banco
- [ ] Índices
- [ ] Teste com grande volume de transações

### Cenário-alvo de teste

O aplicativo deve continuar utilizável com histórico financeiro grande, incluindo milhares de transações por usuário.

---

## 3.8 — Testes de carga e confiabilidade

- [ ] Teste de concorrência
- [ ] Teste de carga da API
- [ ] Teste de banco
- [ ] Teste de filas quando existirem
- [ ] Teste de integração com IA
- [ ] Teste de sincronização
- [ ] Teste de falhas externas
- [ ] Teste de recuperação

---

## 3.9 — Distribuição

- [ ] Configuração de release
- [ ] Assinatura do aplicativo
- [ ] Versionamento
- [ ] Build de produção
- [ ] Testes pré-lançamento
- [ ] Preparação da Play Console
- [ ] Política de privacidade publicada
- [ ] Termos publicados
- [ ] Ficha da loja
- [ ] Ícone
- [ ] Screenshots
- [ ] Descrição
- [ ] Estratégia de atualização

---

# 5. Qualidade e processo de revisão

Toda microtarefa deverá passar por este ciclo:

```text
┌─────────────────────┐
│ Definir microtarefa │
└──────────┬──────────┘
           ↓
┌─────────────────────┐
│ Implementar         │
└──────────┬──────────┘
           ↓
┌─────────────────────┐
│ Testar              │
└──────────┬──────────┘
           ↓
┌─────────────────────┐
│ Debug               │
└──────────┬──────────┘
           ↓
┌─────────────────────┐
│ Revisar arquitetura │
└──────────┬──────────┘
           ↓
┌─────────────────────┐
│ Refatorar           │
└──────────┬──────────┘
           ↓
┌─────────────────────┐
│ Integrar            │
└──────────┬──────────┘
           ↓
┌─────────────────────┐
│ Criar checkpoint    │
└─────────────────────┘
```

## Definition of Done

Uma microtarefa só é considerada concluída quando:

- [ ] A funcionalidade atende ao objetivo
- [ ] O código está organizado
- [ ] O comportamento foi testado
- [ ] Os casos de erro relevantes foram tratados
- [ ] Não existem erros conhecidos críticos
- [ ] Não houve regressão relevante
- [ ] A documentação necessária foi atualizada
- [ ] O próximo passo pode ser iniciado com a base atual

---

# 1.3 — Domínio financeiro

Objetivo: definir as entidades e invariantes financeiras no backend antes de criar tabelas, endpoints ou telas.

### Ordem desta microtarefa

```text
Transaction
  ↓
Account
  ↓
Category
  ↓
Transfer / Payment / Refund
  ↓
Installment / Recurrence
  ↓
Motor financeiro
```

### 1.3.1 — Transaction core

**Status:** CONCLUÍDA

- [x] Definir `TransactionId`, `AccountId` e `CategoryId`
- [x] Definir `Money` em unidade monetária mínima
- [x] Definir código de moeda
- [x] Definir direção `DEBIT/CREDIT`
- [x] Definir tipo de operação
- [x] Definir status
- [x] Definir origem
- [x] Definir data do evento vs. timestamps de auditoria
- [x] Definir referência entre transações relacionadas
- [x] Definir invariantes de validação
- [x] Criar testes unitários
- [x] Validar compatibilidade com futuras parcelas e recorrências

### Invariantes iniciais

- Valor da transação é sempre positivo; a direção determina débito/crédito.
- Valores monetários usam `Long` em unidade mínima, evitando `Double` para dinheiro.
- Operações monetárias exigem a mesma moeda.
- `INCOME` e `REFUND` são créditos.
- `EXPENSE`, `PURCHASE` e `PAYMENT` são débitos.
- `CANCELLATION` exige referência à transação relacionada.
- Descrição é opcional, normalizada e limitada a 160 caracteres.
- Datas financeiras usam `LocalDate`; auditoria usa `Instant`.

**Critério 1.3.1:** domínio compilando no projeto real, testes unitários passando no Gradle do projeto e invariantes documentadas. Validado com `clean test` + `build` e CI.

### Próximas microtarefas 1.3

- [x] 1.3.2 — Account
- [x] 1.3.3 — Category
- [x] 1.3.4 — Transfer
- [x] 1.3.5 — Payment / Refund / Cancellation
- [x] 1.3.6 — Installment
- [x] 1.3.7 — Recurrence
- [x] 1.3.8 — Revisão de domínio

### 1.3.6 — Installment

**Status:** CONCLUÍDA

O agregado representa o cronograma de parcelamento sem lançar automaticamente valores futuros no saldo.

- [x] Divisão exata do total em unidades monetárias mínimas
- [x] Distribuição determinística do resto da divisão
- [x] Cronograma mensal com tratamento correto de fim de mês
- [x] Limite de 2 a 360 parcelas
- [x] Estados do plano e das parcelas
- [x] Cancelamento protegido quando já existe parcela concluída
- [x] Testes unitários e build validados localmente

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

# 6. Estratégia de testes

## Testes unitários

Para:

- regras financeiras
- parsers
- cálculos
- validações
- Use Cases
- regras de domínio

## Testes de integração

Para:

- API + banco
- autenticação
- persistência
- sincronização
- integrações externas

## Testes instrumentados/UI

Para:

- fluxos críticos
- login
- criação de transação
- navegação
- fatura
- metas

## Testes manuais exploratórios

Serão usados para encontrar problemas de UX e cenários que testes automatizados não cobrem bem.

---

# 7. Regras especiais do motor financeiro

O NEXA deve distinguir claramente:

```text
Saldo em conta
≠
Limite do cartão
≠
Limite disponível
≠
Valor da fatura
≠
Valor efetivamente pago
```

Também deverá distinguir:

```text
Compra
Transferência
Pagamento
Estorno
Cancelamento
Parcela
Recorrência
```

Qualquer alteração nessas regras exige novos testes de regressão.

---

# 8. Regras especiais da IA

A IA do NEXA deverá:

- trabalhar sobre contexto financeiro estruturado
- evitar inferências não suportadas pelos dados
- declarar incerteza quando necessário
- não alterar dados financeiros sem confirmação explícita
- não inventar valores, datas ou transações
- respeitar permissões do usuário
- receber somente os dados necessários para a tarefa

A IA será um **assistente de interpretação**, não a fonte primária dos dados financeiros.

---

# 9. Ordem oficial de execução

A sequência oficial será:

```text
PARTE 1
  ↓
1.1 Fechamento técnico
  ↓
1.2 Repositório + ambiente
  ↓
1.3 Domínio financeiro
  ↓
1.4 Banco + migrations
  ↓
1.5 Autenticação
  ↓
1.6 API
  ↓
1.7 Motor financeiro
  ↓
1.8 Integração Android funcional
  ↓
1.9 Design System + refinamento visual
  ↓
1.10 MVP técnico
  ↓
CHECKPOINT PARTE 1
  ↓
PARTE 2
  ↓
2.1 Registro rápido
  ↓
2.2 Dashboard
  ↓
2.3 Contas
  ↓
2.4 Cartões/Faturas
  ↓
2.5 Orçamentos
  ↓
2.6 Metas
  ↓
2.7 Recorrências
  ↓
2.8 Histórico
  ↓
2.9 IA
  ↓
2.10 Notificações
  ↓
CHECKPOINT PARTE 2
  ↓
PARTE 3
  ↓
3.1 Produção
  ↓
3.2 Sincronização
  ↓
3.3 Open Finance
  ↓
3.4 Segurança
  ↓
3.5 LGPD/Compliance
  ↓
3.6 Observabilidade
  ↓
3.7 Performance
  ↓
3.8 Carga/Confiabilidade
  ↓
3.9 Distribuição
  ↓
CHECKPOINT FINAL
```

---

# 10. Regra de evolução do documento

Este arquivo é a **fonte de verdade do planejamento**.

Quando novas decisões estruturais forem tomadas, o plano deverá ser atualizado antes de iniciar uma nova etapa que dependa delas.

Durante a implementação, o checklist será marcado progressivamente.

Alterações no plano deverão preservar a coerência entre:

- arquitetura
- domínio
- banco
- API
- Android
- IA
- segurança
- experiência do usuário

---

# 11. Resultado final esperado

Ao completar as três partes, o NEXA Finance deverá possuir uma base preparada para ser um produto real:

```text
                 NEXA FINANCE
                       │
        ┌──────────────┼──────────────┐
        │              │              │
     Android        Backend           IA
        │              │              │
        └──────────────┼──────────────┘
                       │
                   PostgreSQL
                       │
             ┌─────────┴─────────┐
             │                   │
        Open Finance        Serviços
          futuro             externos
```

A visão do produto é simples:

> **O NEXA deve tornar a gestão financeira pessoal rápida, clara e inteligente, sem sacrificar precisão, segurança ou capacidade de escala.**

---

## Próxima execução oficial

**Confiabilidade operacional → Drill reproduzível de falha externa + recuperação.**

O domínio 1.3 e a fundação de infraestrutura já avançaram além do ponto descrito em versões antigas deste documento. A execução agora deve produzir evidência de comportamento diante de falha externa, validar recuperação sem corromper estado e atualizar o checkpoint antes de abrir a próxima frente.

---

# 1.2 — Repositório e estrutura inicial

## Estado atual

- [x] Monorepo Git inicializado
- [x] `android/` e `backend/` como builds Gradle independentes
- [x] Convenções de repositório documentadas
- [x] `.editorconfig` e `.gitattributes`
- [x] Configuração local via `.env.example`
- [x] Script para geração do Gradle Wrapper
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
- [x] PostgreSQL em execução saudável
- [x] Revisão final da infraestrutura local

### Correção aplicada e resultado

O primeiro `postgres:18-alpine` iniciou em loop de reinício porque o Compose montava o volume em `/var/lib/postgresql/data`. A partir do PostgreSQL 18, a imagem oficial passou a usar `/var/lib/postgresql` como ponto de volume e `PGDATA=/var/lib/postgresql/18/docker`. O Compose foi corrigido para seguir esse contrato e também passou a exigir `POSTGRES_PASSWORD` via `.env`, sem fallback de senha no arquivo versionado.

Após a correção, uma inicialização limpa foi validada: o container ficou `healthy`, `pg_isready` retornou `accepting connections`, e os logs confirmaram `database system is ready to accept connections`. O aviso de locale da imagem Alpine foi identificado como não bloqueante para o ambiente atual.

### Critério de conclusão 1.2

A microtarefa foi concluída após verificar:

1. Android compilando com `assembleDebug`;
2. Backend passando em `clean build` e `test` com JDK 17;
3. Docker operacional;
4. `docker compose ... config` válido;
5. PostgreSQL `healthy` e respondendo a `pg_isready`;
6. `.env` fora do versionamento;
7. revisão final sem pendências bloqueadoras de ambiente, estrutura ou documentação.

**Status: CONCLUÍDA ✅**


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


