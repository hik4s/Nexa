# NEXA Finance — Tokens de elevação

## Objetivo

A elevação do NEXA existe para criar hierarquia espacial, não para decorar a interface com sombras. A maior parte das superfícies deve permanecer visualmente próxima ao plano de fundo.

## Escala

| Token | Valor | Uso orientativo |
|---|---:|---|
| `none` | 0 dp | Superfícies padrão e containers sem destaque |
| `low` | 1 dp | Separação sutil entre superfícies próximas |
| `medium` | 3 dp | Cards ou elementos que precisam de leve destaque |
| `high` | 6 dp | Elementos flutuantes e ações com destaque |
| `overlay` | 12 dp | Camadas temporárias, menus e conteúdo sobreposto |

## Regra de consumo

Novos componentes devem preferir os tokens centralizados:

    CardDefaults.cardElevation(defaultElevation = NexaElevation.medium)

em vez de valores arbitrários:

    CardDefaults.cardElevation(defaultElevation = 5.dp)

## Direção visual

- O padrão é `none`.
- `low` é o primeiro nível de separação perceptível.
- `medium` deve ser suficiente para a maioria dos cards que realmente precisam de elevação.
- `high` fica reservado para elementos flutuantes.
- `overlay` é excepcional e não deve virar o padrão de diálogos ou telas inteiras.

## Observação para Material 3

Os componentes Material 3 possuem estados e elevações próprias. Quando o componente oferecer `CardDefaults`, `ButtonDefaults` ou APIs equivalentes, os tokens NEXA devem alimentar essas propriedades em vez de criar uma segunda escala.

## Estado atual

- Tokens de cor: concluídos
- Tokens tipográficos: concluídos
- Tokens de espaçamento: concluídos
- Tokens de raio: concluídos
- Tokens de elevação: concluídos
- Componentes base: próximos
