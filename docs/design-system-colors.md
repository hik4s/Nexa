# NEXA Design System — cores

## Princípio

O Design System separa:

1. **tokens primitivos**, que representam a identidade visual da marca;
2. **tokens semânticos**, que definem como essas cores devem ser usadas pelos componentes.

Componentes e telas devem consumir papéis semânticos do Material 3 por meio de `MaterialTheme.colorScheme`, e não acessar cores de marca diretamente.

## Tokens primitivos

- Obsidian: `#0B0B0C`
- Ivory: `#F5F2EA`
- Burgundy: `#5A1D2A`
- Deep Green: `#173B2B`
- Soft Gray: `#8C8C8C`
- Champagne: `#C7B27A`

## Mapeamento semântico

### Claro

- `primary`: Burgundy
- `secondary`: Deep Green
- `tertiary`: Champagne
- `background` / `surface`: Ivory
- textos principais: Obsidian
- `outline`: Soft Gray

### Escuro

- `primary`: Burgundy
- `secondary`: Deep Green
- `tertiary`: Champagne
- `background` / `surface`: Obsidian
- textos principais: Ivory
- `outline`: Soft Gray

Champagne permanece terciário para evitar transformar o destaque premium em cor dominante da interface.

## Regra de consumo

Preferir:

`MaterialTheme.colorScheme.primary`

em vez de:

`Burgundy`

Isso permite trocar o comportamento de um componente entre claro e escuro sem espalhar condicionais pela UI.

## Estado atual

Implementado:

- paleta primitiva;
- esquema semântico Material 3 claro;
- esquema semântico Material 3 escuro;
- integração em `NexaTheme`;
- compilação do APK debug validada.

Próximas microtarefas de 1.4:

- tipografia;
- espaçamento;
- raios;
- elevação;
- componentes base;
- estados;
- componentes monetários;
- acessibilidade.
