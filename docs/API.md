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

NEXA Finance API — OK

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

Ainda não implementado neste checkpoint:

- cartões/faturas HTTP
- pagamento de fatura HTTP
- parcelamento HTTP
- estorno completo
- recorrência
- OpenAPI/Swagger servido pela aplicação
- rate limiting e hardening de produção
