# Account — NEXA Finance

`Account` representa uma origem financeira do usuário que pode manter saldo e receber movimentos.

## Tipos

- `CHECKING`
- `SAVINGS`
- `CASH`
- `WALLET`
- `INVESTMENT`
- `OTHER`

Cartões de crédito terão agregado próprio e não serão modelados como `Account`.

## Regras

1. A conta pertence a um `UserId`.
2. Nome normalizado entre 1 e 80 caracteres.
3. A moeda da conta é a moeda do `openingBalance`.
4. `openingBalance` é o saldo inicial conhecido e pode ser positivo, zero ou negativo.
5. O saldo atual não é armazenado como fonte primária; será derivado dos movimentos financeiros.
6. Conta arquivada não pode ser usada para novos movimentos nem renomeada.
7. Arquivamento é reversível.
8. `Account` não referencia `Transaction` diretamente; a relação é feita por `AccountId`.

## Regra futura de integração

No caso de uso que cria uma transação:

```text
Transaction.amount.currency == Account.currency
```

Essa validação ocorrerá fora do agregado para manter baixo acoplamento entre agregados.
