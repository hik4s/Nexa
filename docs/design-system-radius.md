# NEXA Finance — Tokens de raio

## Objetivo

O sistema de raios define a linguagem geométrica dos componentes do NEXA. A direção é deliberadamente contida: cantos arredondados devem transmitir refinamento, não aparência excessivamente lúdica.

## Escala

| Token | Valor | Uso orientativo |
|---|---:|---|
| `none` | 0 dp | Superfícies que precisam permanecer retas |
| `xs` | 4 dp | Ajustes finos e componentes muito compactos |
| `sm` | 8 dp | Controles pequenos e elementos secundários |
| `md` | 12 dp | Raio padrão de componentes |
| `lg` | 16 dp | Cards e superfícies principais |
| `xl` | 24 dp | Containers de destaque e blocos hero |
| `pill` | 999 dp | Chips, badges e controles deliberadamente pill-shaped |

## Regra de consumo

Novos componentes devem consumir `NexaRadii` em vez de declarar valores arbitrários de raio.

Exemplo:

    RoundedCornerShape(NexaRadii.md)

Evitar:

    RoundedCornerShape(13.dp)

## Direção visual

- `md` é o ponto de partida padrão.
- `lg` deve ser usado quando a superfície tiver presença visual maior.
- `xl` é reservado para destaques; não deve virar o padrão de todas as telas.
- `pill` é semântico e deve permanecer limitado a componentes em formato de cápsula.
- Bordas perfeitamente circulares de avatares ou ícones podem usar `CircleShape` diretamente, pois isso representa uma forma semântica, e não um novo token de raio.

## Princípio

Mais arredondamento não significa automaticamente mais qualidade visual. A consistência da escala é mais importante que aumentar o raio de cada componente.

## Estado atual

- Tokens de cor: concluídos
- Tokens tipográficos: concluídos
- Tokens de espaçamento: concluídos
- Tokens de raio: concluídos
- Tokens de elevação: próximos
