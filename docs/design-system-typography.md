# NEXA Design System — tipografia

## Princípios

A tipografia do NEXA prioriza:

1. legibilidade;
2. hierarquia clara;
3. personalidade discreta;
4. destaque controlado para valores financeiros.

A família base é a sans-serif do sistema. Não há dependência de fonte externa nesta etapa.

## Escala

| Token Material 3 | Tamanho | Peso | Uso |
|---|---:|---|---|
| displayLarge | 36sp | SemiBold | destaque financeiro principal |
| displayMedium | 32sp | SemiBold | saldo/resumo de grande porte |
| displaySmall | 28sp | SemiBold | valores e títulos de alto destaque |
| headlineLarge | 26sp | SemiBold | títulos principais de tela |
| headlineMedium | 22sp | SemiBold | títulos de seção |
| headlineSmall | 20sp | SemiBold | cabeçalhos menores |
| titleLarge | 18sp | SemiBold | títulos de cards |
| titleMedium | 16sp | Medium | títulos auxiliares |
| titleSmall | 14sp | Medium | subtítulos |
| bodyLarge | 16sp | Normal | texto principal |
| bodyMedium | 14sp | Normal | texto secundário |
| bodySmall | 12sp | Normal | informação auxiliar |
| labelLarge | 14sp | Medium | ações e labels |
| labelMedium | 12sp | Medium | metadados |
| labelSmall | 11sp | Medium | informação de menor prioridade |

## Regra de consumo

Telas e componentes devem preferir:

`MaterialTheme.typography.headlineMedium`

em vez de criar `TextStyle` local com tamanho/peso arbitrários.

Isso mantém a hierarquia visual consistente e permite ajustes globais sem procurar valores espalhados pelo projeto.

## Valores financeiros

Valores financeiros de maior importância usam a família de estilos `display*` ou `headline*`, conforme o contexto. O significado semântico continua sendo responsabilidade do componente monetário, que será criado em uma microtarefa posterior.

A tipografia não deve ser usada sozinha para indicar positivo/negativo; a semântica visual deve combinar texto, cor e contexto.

## Estado atual

Implementado e validado:

- família sans-serif do sistema;
- escala completa de Typography do Material 3;
- pesos e line heights centralizados;
- integração no `NexaTheme`;
- APK debug compilando com sucesso.
