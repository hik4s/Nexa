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

Cancela uma transação que ainda não esteja cancelada.

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
- saldo derivado das transações publicadas
- isolamento de transações por usuário
- validação de moeda da transação
- validação de escopo da categoria referenciada

Ainda não implementado neste checkpoint:

- transferências HTTP
- cartões/faturas HTTP
- pagamento de fatura HTTP
- parcelamento HTTP
- estorno completo
- recorrência
- OpenAPI/Swagger servido pela aplicação
- rate limiting e hardening de produção
