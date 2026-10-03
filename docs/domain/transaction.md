# Transaction — NEXA Finance

## Objetivo

`Transaction` representa um movimento financeiro que altera o saldo de uma conta.

## Regras

- O valor da transação é sempre positivo; `direction` determina débito/crédito.
- Valores monetários usam `Money` com `Long` em unidade mínima + `CurrencyCode`.
- `INCOME` e `REFUND` são créditos.
- `EXPENSE`, `PURCHASE` e `PAYMENT` são débitos.
- `CANCELLATION` exige `relatedTransactionId`; sua direção será determinada no caso de uso a partir da operação cancelada.
- `TRANSFER` e `ADJUSTMENT` podem ser débito ou crédito conforme o movimento representado.
- Descrição é opcional; quando informada é normalizada e limitada a 160 caracteres.
- Datas financeiras usam `LocalDate`; auditoria usa `Instant`.
- O agregado conhece apenas `AccountId`, evitando acoplamento direto com `Account`.
