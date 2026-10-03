# Payment — NEXA Finance

## Objetivo

Representar um pagamento como uma transação financeira de débito, sem acoplar o domínio atual ao futuro modelo de faturas/cartões.

## Regras

- Valor sempre positivo.
- Direção sempre DEBIT.
- Tipo sempre PAYMENT.
- Status inicial padrão POSTED.
- Status PENDING é permitido.
- Status CANCELLED não pode ser usado na criação.
- relatedTransactionId é opcional para permitir a integração futura com fatura/obrigação financeira.
- Dinheiro usa Money e respeita a moeda informada.

## Implementação

Payment.create(...) cria o agregado e a Transaction correspondente.