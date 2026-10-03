# NEXA Finance — Feedback, loading e error states

## Componentes

| Componente | Uso |
|---|---|
| `NexaLoadingState` | Estado de carregamento de uma seção ou tela |
| `NexaErrorState` | Estado de falha com mensagem e retry opcional |
| `NexaFeedback` | Mensagem contextual curta |

## Tons de feedback

- `Informative`: ação ou informação importante sem conotação positiva/negativa.
- `Positive`: confirmação de operação ou estado saudável.
- `Negative`: erro ou problema que exige atenção.
- `Neutral`: informação secundária sem destaque semântico forte.

## Regras

- Estados devem ser explícitos; uma tela não deve montar seu próprio spinner ou mensagem de erro para casos comuns.
- `NexaErrorState` reutiliza `NexaButton`, mantendo a ação de retry dentro do Design System.
- As cores são derivadas do `MaterialTheme.colorScheme`; nenhuma cor de tela é necessária.
- Mensagens devem ser curtas e acionáveis quando o usuário puder corrigir ou repetir a operação.

## Direção

O NEXA não deve usar feedback visual excessivamente chamativo. O objetivo é orientar o usuário sem interromper desnecessariamente a experiência.

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
- Feedback/loading/error states: concluídos
- Modo claro: próximo
- Modo escuro: próximo
