# NEXA Finance — Modos claro e escuro

## Comportamento

`NexaTheme` usa o modo escuro do sistema operacional por padrão através de `isSystemInDarkTheme()`.

Um chamador ainda pode fornecer `darkTheme` explicitamente quando uma tela, preview ou teste precisar controlar o modo.

## Tema claro

- Fundo principal: Ivory
- Texto principal: Obsidian
- Primary: Burgundy
- Secondary: Deep Green
- Accent: Champagne
- Superfícies secundárias usam uma escala neutra derivada do Ivory

## Tema escuro

- Fundo principal: Obsidian
- Texto principal: Ivory
- Primary/Secondary/Tertiary preservam a identidade sem transformar a interface em uma composição multicolorida
- Superfícies secundárias usam uma escala neutra derivada do Obsidian

## Regra

Componentes devem consumir `MaterialTheme.colorScheme` e nunca verificar `darkTheme` diretamente para decidir cores locais.

Isso mantém a lógica do tema concentrada em uma única camada e permite que a mesma tela funcione nos dois modos.

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
- Modo claro: concluído
- Modo escuro: concluído
- Acessibilidade básica: concluída via Android Lint
