# Revisão de domínio — 1.3.8

## Resultado

A revisão do domínio financeiro identificou uma inconsistência de tipagem e duas regras que devem permanecer explícitas nas camadas corretas.

## Correção aplicada

### CategoryId único

O projeto possuía dois conceitos com o mesmo nome:

- `category.CategoryId`: `value class` sobre UUID.
- `transaction.CategoryId`: `typealias UUID`.

Isso permitia que transações e categorias usassem identificadores com contratos diferentes para o mesmo conceito.

A referência oficial passa a ser `com.nexa.finance.domain.category.CategoryId`, usada por:

- `Transaction`
- `Installment`
- `Recurrence`
- `Category`

Isso reforça a tipagem do domínio e reduz o risco de misturar UUIDs arbitrários com identificadores de categoria.

## Regras confirmadas

### Transaction

- valor sempre positivo;
- direção determina débito/crédito quando o tipo possui semântica fixa;
- descrição opcional, normalizada e limitada a 160 caracteres;
- datas financeiras usam `LocalDate`;
- auditoria usa `Instant`;
- cancelamento de transação exige referência ao lançamento relacionado.

A criação de uma transação de cancelamento continua centralizada em `Cancellation`, que calcula a direção oposta ao lançamento original.

### Account

- conta possui proprietário;
- contas arquivadas não podem ser usadas para novos lançamentos;
- nome é normalizado e limitado a 80 caracteres.

### Category

- categorias podem ser de sistema ou de usuário;
- categorias de sistema não podem ser alteradas pelo usuário;
- categorias arquivadas não podem ser usadas em novos lançamentos;
- a hierarquia impede uma categoria de ser seu próprio pai.

A validação de existência, propriedade do pai e compatibilidade de tipos entre categorias relacionadas depende de carregar as entidades envolvidas e deve ser aplicada pela camada de aplicação/repositório.

### Transfer

- origem e destino devem ser contas diferentes do mesmo usuário;
- a moeda precisa ser a mesma;
- o lançamento de origem é débito e o de destino é crédito;
- os dois lançamentos possuem referências mútuas;
- os estados do agregado e dos dois lançamentos devem permanecer sincronizados.

### Payment / Refund / Cancellation

`Payment` representa um débito.

`Refund` representa um crédito ligado a uma despesa/compra original e não pode individualmente exceder o valor original.

`Cancellation` para lançamento POSTED cria um lançamento inverso; para lançamento PENDING realiza apenas a transição de estado.

### Regra de estornos acumulados

Uma instância isolada de `Refund` não possui informação suficiente para garantir que a soma de vários refunds do mesmo lançamento não ultrapasse o valor original.

Portanto, a seguinte invariante deve ser aplicada no Use Case/repositório que cria refunds:

`sum(posted refunds for original) <= original amount`

Essa regra não deve ser duplicada de forma parcial em cada instância de `Refund`.

### Installment

- plano entre 2 e 360 parcelas;
- soma exata do total;
- divisão determinística do resto monetário;
- datas mensais calculadas a partir da data âncora;
- plano não cria automaticamente lançamentos futuros.

### Recurrence

- frequências diária, semanal, mensal e anual;
- intervalo de 1 a 365;
- limites opcionais por data e/ou quantidade;
- cálculo ancorado na data inicial;
- tratamento de fim de mês e ano bissexto sem deriva permanente;
- pausa, retomada e cancelamento;
- ocorrências futuras não são automaticamente lançamentos contábeis.

## Separação de responsabilidades

O domínio define invariantes internas e transições de estado.

A camada de aplicação deve garantir regras que dependem de múltiplos agregados ou do histórico persistido, como:

- existência e proprietário de contas/categorias;
- compatibilidade entre categoria e operação;
- soma acumulada de refunds;
- idempotência ao materializar ocorrências de recorrências;
- materialização de parcelas/recorrências em Transactions.

## Conclusão

A revisão não encontrou necessidade de alterar o modelo contábil adotado para parcelas e recorrências. A principal correção estrutural desta revisão foi unificar `CategoryId` como identificador fortemente tipado.
