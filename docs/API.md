# NEXA Finance API

Versão atual: v1

Base path: /api/v1

## Autenticação

Endpoints protegidos usam:

Authorization: Bearer <session-token>

O token é emitido no login e revogado no logout ou quando a senha é redefinida.

## Respostas de erro

Erros de validação usam:

    {
      "code": "VALIDATION_ERROR",
      "message": "..."
    }

Corpos inválidos usam o mesmo contrato VALIDATION_ERROR com a mensagem Request body is invalid.

Erros inesperados usam HTTP 500 com código INTERNAL_ERROR, sem expor detalhes internos.

## Auth

### POST /auth/register

Cria um usuário.

Request:

    {
      "email": "user@example.com",
      "displayName": "NEXA User",
      "password": "strong-password",
      "timeZone": "UTC"
    }

Resposta: 201 Created.

### POST /auth/login

Autentica um usuário e cria uma sessão.

Request:

    {
      "email": "user@example.com",
      "password": "strong-password"
    }

Resposta: 200 OK.

### POST /auth/logout

Protegido. Revoga a sessão atual.

Resposta: 204 No Content.

### GET /auth/me

Protegido. Retorna o perfil autenticado.

### PUT /auth/me

Protegido. Atualiza campos fornecidos do perfil.

Request:

    {
      "displayName": "Novo nome",
      "email": "new@example.com",
      "timeZone": "America/Sao_Paulo"
    }

### POST /auth/recovery/request

Solicita recuperação de acesso.

O token retornado atualmente existe para permitir validação do fluxo enquanto não há provedor de e-mail integrado. Antes de produção, o token deverá ser entregue por canal externo seguro e o endpoint deverá evitar enumeração de contas.

### POST /auth/recovery/reset

Consome o token de recuperação e substitui a credencial.

Request:

    {
      "token": "...",
      "newPassword": "new-strong-password"
    }

Resposta: 204 No Content.

A redefinição invalida as sessões ativas do usuário.

## Accounts

Todos os endpoints abaixo são protegidos.

### POST /accounts

Cria uma conta financeira pertencente exclusivamente ao usuário autenticado.

Request:

    {
      "name": "NEXA Checking",
      "type": "CHECKING",
      "openingBalanceMinor": 100000,
      "currency": "BRL"
    }

Resposta: 201 Created.

### GET /accounts

Lista somente contas do usuário autenticado.

### GET /accounts/{id}

Retorna somente uma conta pertencente ao usuário autenticado.

A consulta SQL aplica owner_id = authenticatedUserId, portanto um usuário não consegue recuperar uma conta de outro usuário apenas conhecendo o UUID.

## Health

### GET /health

Endpoint público para verificar disponibilidade básica do processo.

Resposta:

NEXA Finance API - OK

## Transactions

Todos os endpoints abaixo são protegidos.

### POST /transactions

Cria uma transação manual na conta do usuário autenticado.

Request:

    {
      "accountId": "uuid",
      "amountMinor": 12500,
      "currency": "BRL",
      "type": "EXPENSE",
      "direction": "DEBIT",
      "occurredOn": "2026-10-03",
      "description": "Mercado",
      "categoryId": null,
      "source": "MANUAL"
    }

A conta precisa pertencer ao usuário, estar ativa e usar a mesma moeda da transação.

### GET /transactions

Lista as transações pertencentes ao usuário autenticado. O parâmetro opcional accountId filtra por conta.

### GET /transactions/{id}

Retorna somente uma transação pertencente ao usuário autenticado.

### POST /transactions/{id}/post

Publica uma transação pendente.

### POST /transactions/{id}/cancel

Coloca a transação como cancelada se ela ainda não estiver cancelada.

## Transfers

Todos os endpoints abaixo são protegidos.

### POST /transfers

Cria uma transferência entre duas contas ativas do usuário autenticado. A operação cria duas transações vinculadas: débito na origem e crédito no destino. A transferência começa como `PENDING`.

Request:

    {
      "sourceAccountId": "uuid",
      "destinationAccountId": "uuid",
      "amountMinor": 10000,
      "currency": "BRL",
      "occurredOn": "2026-10-03",
      "source": "MANUAL"
    }

As contas precisam pertencer ao mesmo usuário, estar ativas e usar a mesma moeda. A conta de origem e a conta de destino não podem ser iguais. A persistência da transferência e das duas transações é atômica no PostgreSQL.

### GET /transfers

Lista as transferências do usuário autenticado.

### GET /transfers/{id}

Retorna somente uma transferência pertencente ao usuário autenticado.

### POST /transfers/{id}/post

Publica a transferência e publica as duas transações vinculadas.

### POST /transfers/{id}/cancel

Cancela a transferência pendente e cancela as duas transações vinculadas. Uma transferência cancelada não altera o saldo porque somente transações `POSTED` entram no saldo.

## Estado de implementação

Implementado e validado:

- cadastro
- login
- sessão
- logout
- recuperação e redefinição de senha
- perfil
- autenticação Bearer
- erros HTTP padronizados
- contas: criação, listagem e consulta
- isolamento de contas por usuário
- transações: criação, listagem, consulta e cancelamento
- transferências: criação, listagem, consulta, publicação e cancelamento
- saldo derivado das transações publicadas
- isolamento de transações por usuário
- validação de moeda da transação
- validação de escopo da categoria referenciada

Status do checkpoint: parcelamento, recorrência e rate limiting já possuem implementação HTTP. OpenAPI/Swagger servido pela aplicação permanece em aberto.

Implementado e validado neste projeto:
- endpoints de parcelamento/installments
- endpoints de recorrência
- rate limiting por cliente + resposta 429
Em aberto: OpenAPI/Swagger servido pela aplicação e integrações externas de produção.
## Cartões

Todos os endpoints abaixo são protegidos.

### POST /cards

Cria um cartão de crédito vinculado a uma conta de passivo existente do tipo `CREDIT_CARD`. A conta de passivo precisa pertencer ao usuário autenticado e estar ativa.

### GET /cards

Lista os cartões do usuário autenticado, incluindo limite disponível calculado pelo saldo do passivo.

### GET /cards/{id}

Retorna um cartão pertencente ao usuário autenticado.

### POST /cards/{id}/status

Altera o estado entre `ACTIVE`, `BLOCKED` e `CANCELLED`, respeitando as transições válidas do domínio.

### POST /cards/{id}/purchases

Registra uma compra no passivo do cartão. O valor precisa caber no limite disponível e a moeda precisa coincidir com a moeda do cartão. Por padrão a compra é publicada imediatamente; `post=false` mantém a transação `PENDING`.

## Faturas

### POST /invoices

Gera uma fatura para um cartão e período. São consolidadas as transações `POSTED` de `PURCHASE` e `REFUND` ainda não faturadas no período. Não é permitido criar uma segunda fatura ativa para o mesmo cartão e período.

### GET /invoices

Lista as faturas dos cartões do usuário autenticado.

### GET /invoices/{id}

Retorna somente uma fatura pertencente ao usuário autenticado.

### POST /invoices/{id}/close

Fecha uma fatura `OPEN`, tornando-a apta para pagamento.

### POST /invoices/{id}/overdue

Marca uma fatura `CLOSED` como `OVERDUE`.

### POST /invoices/{id}/payments

Registra o pagamento de uma fatura com uma conta não-cartão. A operação é atômica: débito na conta de origem e débito no passivo do cartão, com as duas transações vinculadas, atualização da fatura e registro do pagamento. Pagamentos parciais mantêm a fatura aberta para quitação; o pagamento integral muda o status para `PAID`.

## Reembolsos e cancelamentos contábeis

Todos os endpoints abaixo são protegidos.

### POST /transactions/{id}/refunds

Cria um reembolso contábil para uma transação POSTED do tipo EXPENSE ou PURCHASE.

Request:

    {
      "amountMinor": 2000,
      "currency": "BRL",
      "occurredOn": "2026-10-04",
      "description": "Reembolso parcial"
    }

Reembolsos parciais são permitidos, mas a soma dos reembolsos ativos não pode ultrapassar o valor original. O reembolso referencia a transação original por relatedTransactionId.

Para compras de cartão, o reembolso é lançado como DEBIT no passivo do cartão, reduzindo a dívida e restaurando o limite disponível. O reembolso de uma despesa de conta comum é lançado como CREDIT.

O reembolso de cartão continua permitido quando o cartão está BLOCKED ou CANCELLED, pois bloqueio/cancelamento impede novas compras, não a correção contábil de uma compra existente.

### POST /transactions/{id}/cancellation

Cria uma transação CANCELLATION POSTED para reverter contabilmente uma transação publicada.

Request:

    {
      "occurredOn": "2026-10-04",
      "description": "Correção"
    }

A direção da transação de cancelamento é oposta à transação original. Um mesmo evento só pode possuir uma cancellation ativa; a regra também é protegida por índice único parcial no PostgreSQL.

Transações de transferência são revertidas pelo agregado de transferência. Transações que participam de pagamento de fatura são revertidas pelo agregado da fatura para preservar a operação de duas pernas.

A operação não apaga nem altera a transação original: registra um novo evento contábil relacionado a ela.

## Estado de implementação

Neste checkpoint também estão implementados e validados:

- cartões: criação, consulta, listagem, status e compras;
- faturas: geração, consulta, fechamento, vencimento e pagamentos parciais/integrais;
- reembolsos parciais e integrais;
- reembolso de compras de cartão;
- cancelamento contábil de transações publicadas;
- proteção contra reembolso acima do valor original;
- proteção contra cancellation duplicada;
- isolamento por usuário nos fluxos de refund/cancellation;
- validação contra PostgreSQL real.


## Recorrências

Endpoints protegidos para regras de lançamentos recorrentes.

- POST /api/v1/recurrences - cria uma recorrência de INCOME ou EXPENSE.
- GET /api/v1/recurrences - lista somente as recorrências do usuário autenticado.
- GET /api/v1/recurrences/{id} - consulta uma recorrência pertencente ao usuário.
- GET /api/v1/recurrences/{id}/occurrences?from=YYYY-MM-DD&to=YYYY-MM-DD - calcula ocorrências determinísticas no intervalo inclusivo.
- POST /api/v1/recurrences/{id}/pause - pausa uma recorrência ACTIVE.
- POST /api/v1/recurrences/{id}/resume - retoma uma recorrência PAUSED.
- POST /api/v1/recurrences/{id}/cancel - cancela uma recorrência.
- POST /api/v1/recurrences/{id}/occurrences/{number}/execute - materializa uma ocorrência como Transaction SYSTEM.

A execução usa recurrence_id + recurrence_occurrence_number como identidade da ocorrência no ledger. O PostgreSQL possui índice único parcial para impedir materialização duplicada. Repetir a execução da mesma ocorrência retorna a transação já criada, mantendo a operação idempotente no fluxo normal.

Recorrências validam proprietário da conta, conta ACTIVE, moeda e categoria. Contas de cartão de crédito não podem ser usadas diretamente por recorrências INCOME/EXPENSE; compras recorrentes de cartão devem usar a semântica de Card/Purchase.

A execução usa CREDIT para INCOME e DEBIT para EXPENSE e registra a data agendada da ocorrência.
