# NEXA Finance — Cards

## Variantes

| Variante | Uso |
|---|---|
| `Standard` | Superfície principal, sem sombra perceptível |
| `Elevated` | Bloco que precisa de separação espacial discreta |
| `Outlined` | Quando a borda precisa definir claramente os limites |

## Comportamento

`NexaCard` pode ser estático ou clicável. Quando `onClick` é fornecido, o próprio Material 3 gerencia interação, foco e feedback.

## Regras visuais

- Raio padrão: `NexaRadii.lg`.
- Standard e Outlined usam `NexaElevation.none`.
- Elevated usa `NexaElevation.medium`.
- A superfície deriva de `MaterialTheme.colorScheme.surface`.
- A borda de Outlined vem da API Material 3.

## Regra de uso

Use `NexaCard` para agrupar conteúdo relacionado como saldo, resumos, contas e itens de informação. Não usar cards apenas para adicionar uma borda ou sombra sem criar uma relação visual útil.

Exemplo:

    NexaCard(variant = NexaCardVariant.Elevated) {
        // conteúdo
    }

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
- Componentes de valor monetário: próximos
