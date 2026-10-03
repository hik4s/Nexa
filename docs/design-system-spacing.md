# NEXA Finance — Tokens de espaçamento

## Objetivo

O espaçamento do NEXA deve criar ritmo visual consistente, reduzir decisões arbitrárias e manter a interface alinhada ao conceito de minimalismo premium.

A escala usa uma base de **4 dp** e concentra os valores mais comuns em uma sequência curta.

## Escala

| Token | Valor | Uso orientativo |
|---|---:|---|
| `xxs` | 4 dp | Separações mínimas, ícone + texto, ajustes finos |
| `xs` | 8 dp | Espaçamento interno pequeno |
| `sm` | 12 dp | Agrupamentos compactos |
| `md` | 16 dp | Espaçamento padrão de componentes |
| `lg` | 20 dp | Separação entre elementos relacionados |
| `xl` | 24 dp | Padding de seção e separação de blocos |
| `xxl` | 32 dp | Separação entre grupos principais |
| `xxxl` | 40 dp | Respiro de seção |
| `huge` | 48 dp | Grandes divisões de conteúdo |
| `hero` | 64 dp | Áreas de destaque e composição |

## Regra de consumo

Novos componentes devem preferir um token centralizado, por exemplo:

    Modifier.padding(NexaSpacing.md)

em vez de introduzir um valor arbitrário:

    Modifier.padding(17.dp)

Valores fora da escala podem existir quando uma necessidade específica de plataforma ou layout justificar, mas devem ser exceção e não criar uma segunda escala paralela.

## Princípio de composição

Os tokens são **primitivos**. Não criar tokens duplicados como `cardPadding` ou `screenPadding` diretamente nesta camada.

Quando os componentes base forem criados, decisões semânticas de layout devem combinar os tokens existentes. Isso mantém a escala pequena e previsível.

## Estado atual

- Tokens de cor: concluídos
- Tokens tipográficos: concluídos
- Tokens de espaçamento: concluídos
- Tokens de raio: próximos
- Tokens de elevação: próximos
