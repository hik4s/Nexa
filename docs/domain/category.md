# Category — NEXA Finance

`Category` representa uma classificação financeira usada para organizar transações.

## Categorias do sistema e personalizadas

O NEXA suporta os dois tipos:

- **Sistema**: `ownerId == null`; catálogo fornecido pelo NEXA e imutável pelo usuário.
- **Personalizada**: pertence a um `UserId` e pode ser renomeada, ter ícone alterado, arquivada e restaurada.

## Tipos

- `EXPENSE`
- `INCOME`
- `BOTH`

## Regras

1. Nome normalizado entre 1 e 60 caracteres.
2. `sortOrder` não pode ser negativo.
3. `parentId` é opcional.
4. Uma categoria não pode ser sua própria mãe.
5. O ciclo completo da hierarquia será validado pela camada de aplicação/persistência quando a árvore já puder ser consultada.
6. Categorias arquivadas permanecem no histórico e não podem ser usadas em novas transações.
7. Categorias arquivadas podem ser restauradas.
8. Categorias do sistema não podem ser alteradas pelo usuário.
9. A camada de aplicação deve validar que uma categoria personalizada pertence ao usuário atual.
10. A camada de aplicação/persistência deverá validar a compatibilidade e o pertencimento do `parentId` (mesmo proprietário ou categoria de sistema).
11. Unicidade de nomes entre irmãos é responsabilidade da persistência/serviço, não do agregado isolado.

## Decisão de histórico

O NEXA não excluirá categorias referenciadas por transações de forma destrutiva. O arquivamento preserva a integridade histórica.
