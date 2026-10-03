# Refund — NEXA Finance

## Objetivo

Representar um estorno como um crédito relacionado à transação financeira original.

## Regras

- A transação original deve estar POSTED.
- A original deve ser EXPENSE ou PURCHASE.
- A original deve ser um débito.
- O estorno é REFUND + CREDIT.
- O estorno referencia a transação original via relatedTransactionId.
- Moeda deve ser a mesma da transação original.
- Valor deve ser positivo.
- Estorno parcial é permitido.
- O valor do estorno não pode superar o valor original.
- Estorno não nasce CANCELLED.

## Implementação

Refund.create(original, amount, ...) cria uma nova transação de crédito sem alterar a transação histórica original.