# Cancellation — NEXA Finance

## Objetivo

Representar cancelamentos sem apagar o histórico financeiro.

## Dois cenários

### Cancelamento de transação pendente

Cancellation.cancelPending(...) realiza apenas a transição de estado: PENDING → CANCELLED.
Não cria um novo movimento financeiro.

### Cancelamento de transação publicada

Cancellation.createForPosted(...) cria uma nova transação compensatória:

- tipo CANCELLATION;
- mesma conta;
- mesmo valor;
- mesma data financeira;
- referência para a transação original;
- direção oposta à original;
- status POSTED;
- origem SYSTEM.

A transação original permanece preservada.

## Regras

- Somente transações POSTED podem gerar uma transação de cancelamento.
- Uma transação CANCELLATION não pode ser cancelada novamente por este mecanismo.
- Cancelamento de uma transação de débito gera crédito.
- Cancelamento de uma transação de crédito gera débito.