# NEXA Finance — Componentes base

## Objetivo

Os componentes base são primitivas de composição visual. Eles encapsulam as decisões já tomadas nos tokens e evitam que telas criem estilos locais concorrentes.

## Componentes

| Componente | Responsabilidade |
|---|---|
| `NexaSurface` | Superfície Material 3 com shape, cor e elevação padronizados |
| `NexaDivider` | Separador horizontal com cor neutra do tema |
| `NexaSection` | Ritmo vertical e título padrão para blocos de conteúdo |

## Regras

- Componentes base não conhecem regras de negócio.
- Valores visuais devem vir de `MaterialTheme`, `NexaSpacing`, `NexaRadii` e `NexaElevation`.
- Cards, botões, campos e valores monetários terão componentes próprios nas próximas microtarefas.
- Uma tela não deve recriar essas primitivas apenas para alterar um detalhe visual sem justificativa.

## Estado atual

- Tokens de cor: concluídos
- Tokens tipográficos: concluídos
- Tokens de espaçamento: concluídos
- Tokens de raio: concluídos
- Tokens de elevação: concluídos
- Componentes base: concluídos
- Estados de botão: próximo
- Campos de entrada: próximos
- Cards: próximos
