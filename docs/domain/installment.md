# Installment — domínio

O agregado Installment representa um plano de parcelamento e seu cronograma. Ele não lança automaticamente valores no saldo. A contabilização de cada parcela será responsabilidade das camadas de aplicação/invoice quando esses conceitos forem implementados.

## Regras

- installmentCount entre 2 e 360.
- O valor total deve ser positivo e conter ao menos uma unidade monetária mínima por parcela.
- Todas as parcelas usam a mesma moeda do total.
- A soma das parcelas é exatamente igual ao total.
- Quando o total não divide igualmente, as primeiras parcelas recebem a unidade monetária excedente.
- As datas são mensais e preservam o dia original quando possível; meses menores usam o último dia disponível e o mês seguinte volta ao dia original quando ele existir.
- O plano começa ACTIVE.
- Uma parcela concluída passa para COMPLETED; quando todas estão concluídas, o plano também fica COMPLETED.
- Um plano sem parcelas concluídas pode ser cancelado, marcando todas as parcelas como CANCELLED.
- Um plano com parcelas já concluídas não pode ser cancelado diretamente; deverá usar uma operação de reversão/estorno específica.
- A descrição é opcional, normalizada e limitada a 160 caracteres.

## Separação contábil

Uma parcela planejada não é automaticamente uma Transaction. Isso evita reconhecer no saldo valores futuros antes da confirmação financeira. A ligação entre parcelas, faturas e lançamentos será definida nas próximas camadas do domínio.