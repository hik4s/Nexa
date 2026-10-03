# Revisão de domínio — 1.6

## Estado

A maior parte do modelo financeiro já está implementada e testada. A etapa permanece em andamento porque `Card` e `Invoice` exigem uma decisão contábil antes de serem cristalizados como agregados.

## Entidades concluídas

- `User`: identidade do usuário, perfil básico, timezone e estados de ciclo de vida. Credenciais e sessão permanecem na Parte 1.7.
- `Account`: conta financeira.
- `Transaction`: evento financeiro principal.
- `Category`: categorias de sistema e usuário.
- `PaymentMethodType`: vocabulário conceitual de meios de pagamento; não é um agregado persistente independente.
- `Transfer`: transferência entre contas.
- `Payment`: débito de pagamento.
- `Refund`: crédito ligado a uma despesa/compra original.
- `Cancellation`: reversão de lançamento.
- `Installment`: planejamento de parcelamento.
- `Budget`: limite financeiro por período, opcionalmente por categoria; gasto consumido será derivado de Transactions.
- `Goal`: meta financeira com target, progresso, contribuições e ciclo de vida.
- `Recurrence`: agenda de lançamentos recorrentes; cobre conceitualmente `RecurringTransaction`.

## Card e Invoice

Esses agregados ainda não foram implementados porque o domínio precisa decidir como a dívida do cartão entra no modelo contábil.

Existem duas estruturas plausíveis:

1. **Cartão como instrumento ligado a uma conta de passivo:** compras geram lançamentos no ledger do cartão e a fatura consolida esses lançamentos.
2. **Cartão como subledger independente:** o cartão mantém sua própria dívida e a fatura é um agregado de consolidação que depois gera um pagamento contra uma conta financeira.

A decisão altera `Account`, `Transaction`, `Card`, `Invoice`, pagamento de fatura, limite disponível e o motor de saldo. Por isso não é uma boa prática escolher silenciosamente neste checkpoint.

## Regras já consolidadas

- Dinheiro usa unidades mínimas de moeda.
- Datas financeiras usam `LocalDate`; timestamps de auditoria usam `Instant`.
- Transações têm valor positivo e direção explícita.
- Regras que dependem do histórico persistido continuam na aplicação/repositório.
- Estornos acumulados continuam sendo responsabilidade da camada de aplicação.
- Parcelas e recorrências permanecem agendas, não lançamentos automáticos.

## Próximo bloqueio

Para concluir 1.6, precisamos definir qual arquitetura de ledger representa a dívida do cartão e como a fatura participa do saldo.