# NEXA Finance — Estados de botão

## Variantes

| Variante | Papel |
|---|---|
| `Primary` | Ação principal e confirmação |
| `Secondary` | Ação alternativa com menor peso visual |
| `Tonal` | Ações relacionadas que precisam de preenchimento suave |
| `Text` | Ação contextual de baixa ênfase |

## Estados

O componente centraliza três condições:

- **Enabled:** ação disponível.
- **Disabled:** ação indisponível e não interativa.
- **Loading:** ação temporariamente bloqueada enquanto uma operação é processada; o conteúdo é substituído por um indicador.

Quando `loading = true`, o componente também usa `effectiveEnabled = enabled && !loading`, evitando cliques duplicados.

## Forma

- Altura mínima: 48 dp, aproveitando `NexaSpacing.huge` como área mínima do controle.
- Raio: `NexaRadii.md`.
- Padding horizontal: `NexaSpacing.lg`.
- Padding vertical: `NexaSpacing.sm`.
- Cores: derivadas do `MaterialTheme.colorScheme`.

## Regra de uso

Use `NexaButton` em vez de aplicar `Button`, `OutlinedButton`, `FilledTonalButton` ou `TextButton` diretamente nas telas quando a intenção for um botão de ação padrão do produto.

Exemplo:

    NexaButton(
        onClick = ::save,
        loading = state.isSaving,
    ) {
        Text("Salvar")
    }

## Estados de interação

Hover, focus, pressed e ripple permanecem sob responsabilidade do Material 3. O NEXA padroniza principalmente a estrutura, variante, geometria e comportamento de carregamento.

## Estado atual

- Tokens de cor: concluídos
- Tokens tipográficos: concluídos
- Tokens de espaçamento: concluídos
- Tokens de raio: concluídos
- Tokens de elevação: concluídos
- Componentes base: concluídos
- Estados de botão: concluídos
- Campos de entrada: próximos
- Cards: próximos
