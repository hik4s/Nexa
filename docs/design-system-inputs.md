# NEXA Finance — Campos de entrada

## Variantes

| Variante | Uso |
|---|---|
| `Outlined` | Padrão para formulários e dados financeiros |
| `Filled` | Campos em superfícies onde uma separação mais suave é desejada |

## Estados

`NexaTextField` centraliza os seguintes estados:

- **Enabled:** campo disponível para edição.
- **Focused:** interação visual administrada pelo Material 3, com destaque pela cor primária do tema.
- **Error:** borda/indicador, label e texto de suporte assumem a cor semântica de erro.
- **Disabled:** campo não interativo; o componente respeita os estados de contraste fornecidos pelo Material 3.
- **Read-only:** conteúdo permanece visível sem permitir edição.

## Recursos

- label
- placeholder
- supporting text
- leading icon
- trailing icon
- transformação visual, incluindo senhas
- `KeyboardOptions` e `KeyboardActions`
- campo de uma linha por padrão

## Regra de uso

Telas de cadastro, filtros, busca e lançamentos devem preferir `NexaTextField` em vez de estilizar diretamente `TextField` ou `OutlinedTextField`.

Exemplo:

    NexaTextField(
        value = state.description,
        onValueChange = viewModel::onDescriptionChanged,
        label = "Descrição",
        isError = state.descriptionError != null,
        supportingText = state.descriptionError,
    )

## Direção visual

- Raio padrão: `NexaRadii.md`.
- Cor de foco: `MaterialTheme.colorScheme.primary`.
- Erro: `MaterialTheme.colorScheme.error`.
- Nenhuma cor específica de tela deve ser necessária para estados comuns.

## Acessibilidade

Os componentes continuam baseados nas APIs Material 3 para preservar semântica, foco e interação de acessibilidade. Labels devem ser informativos e erros devem ser acompanhados de supporting text quando houver orientação útil ao usuário.

## Estado atual

- Tokens de cor: concluídos
- Tokens tipográficos: concluídos
- Tokens de espaçamento: concluídos
- Tokens de raio: concluídos
- Tokens de elevação: concluídos
- Componentes base: concluídos
- Estados de botão: concluídos
- Campos de entrada: concluídos
- Cards: próximos
