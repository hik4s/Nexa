# NEXA Finance — Transaction test compatibility fix

Este patch corrige somente `TransactionTest.kt` para refletir o contrato atual de `Transaction`.

## Corrigido
- remove `clock`, que não pertence à API atual de `Transaction.create()`;
- ajusta a ordem/nome dos parâmetros para `type` → `direction`;
- ajusta o status padrão para `POSTED`;
- usa `createdAt`/`updatedAt` explícitos para testes determinísticos;
- cobre `PENDING -> POSTED` e cancelamento como cópias imutáveis;
- adiciona teste para valor negativo e cancelamento com referência.

Não altera o código de produção.
