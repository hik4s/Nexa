# NEXA Finance — Valores monetários

## Objetivo

`NexaMoneyText` centraliza a apresentação de valores financeiros sem usar ponto flutuante. O valor recebido pela UI é expresso em unidades monetárias mínimas (`minor units`), preservando precisão determinística.

## API

- `amountInMinorUnits`: valor inteiro em unidade mínima da moeda.
- `currencyCode`: código ISO da moeda, com `BRL` como padrão.
- `tone`: semântico e explícito (`Neutral`, `Income`, `Expense`, `Accent`).
- `showSign`: exibe `+` para valores positivos quando necessário.
- `style`: estilo tipográfico, com `headlineSmall` como padrão.

## Formatação

A quantidade de casas decimais é derivada da moeda. Para BRL, por exemplo, `1890` representa `R$ 18,90`.

Valores negativos preservam o sinal da própria quantia. O componente não tenta inferir receita ou despesa a partir do sinal; essa informação é semântica e deve ser fornecida por `tone` quando relevante.

## Regra de uso

Fluxos financeiros devem preferir `NexaMoneyText` em vez de formatar moeda diretamente dentro de cada tela.

Exemplo:

    NexaMoneyText(
        amountInMinorUnits = 1890,
        tone = NexaMoneyTone.Expense,
    )

## Testes

A função pura `formatNexaMoney` possui testes unitários para:

- BRL positivo
- BRL negativo
- sinal positivo explícito

## Estado atual

- Tokens de cor: concluídos
- Tokens tipográficos: concluídos
- Tokens de espaçamento: concluídos
- Tokens de raio: concluídos
- Tokens de elevação: concluídos
- Componentes base: concluídos
- Estados de botão: concluídos
- Campos de entrada: concluídos
- Cards: concluídos
- Componentes de valor monetário: concluídos
