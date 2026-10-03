# Recurrence — domínio

O agregado Recurrence representa uma regra de lançamento financeiro futuro e seu cronograma determinístico. Ele não cria automaticamente Transaction nem altera saldo; a materialização contábil ficará nas camadas de aplicação quando o fluxo de recorrências for integrado ao motor financeiro.

## Regras

- O valor recorrente é sempre positivo.
- A recorrência usa uma conta financeira e suporta, nesta etapa, apenas `INCOME` e `EXPENSE`.
- A frequência pode ser diária, semanal, mensal ou anual.
- O intervalo é inteiro entre 1 e 365 unidades da frequência.
- A data inicial é a âncora de todos os cálculos.
- A recorrência pode ser indefinida ou limitada por `endOn`, `maxOccurrences`, ou ambos.
- `maxOccurrences` fica entre 1 e 10.000 quando informado.
- Datas mensais preservam o dia original quando possível; meses menores usam o último dia disponível e o mês seguinte volta ao dia original quando existir.
- Datas anuais preservam mês e dia da âncora; 29 de fevereiro volta a existir em anos bissextos futuros.
- A descrição é opcional, normalizada e limitada a 160 caracteres.
- A recorrência inicia em `ACTIVE` e pode ser `PAUSED`, `ACTIVE` novamente ou `CANCELLED`.
- Recorrências pausadas ou canceladas não produzem ocorrências executáveis.
- Pausar ou cancelar não reescreve a regra nem materializa lançamentos retroativos.
- Consultas de ocorrência por número permanecem determinísticas para auditoria, independentemente do status.

## Separação contábil

Uma ocorrência agendada não é automaticamente uma Transaction. Isso evita gerar lançamentos, receitas ou despesas no saldo somente porque uma regra futura foi cadastrada. A materialização de uma ocorrência em Transaction será responsabilidade de um Use Case/camada de aplicação posterior, com idempotência e referência à recorrência.

## Comportamento de datas

O cálculo usa sempre a data inicial como âncora. Isso evita o problema de deriva causado por calcular a próxima data a partir de uma data já ajustada.

Exemplo mensal iniciado em 31 de janeiro:

`31/01 → 28/02 → 31/03 → 30/04`

A ocorrência de abril continua sendo calculada a partir do dia 31 original, e não de 28 ou 30.

## Limites desta microtarefa

Ainda não existe:

- geração automática de Transaction;
- política de cobrança retroativa após pausa;
- notificações;
- detecção automática de recorrências a partir do histórico;
- recorrências específicas de cartão/fatura.

Esses comportamentos pertencem às próximas camadas do produto.
