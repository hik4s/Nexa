# NEXA Finance — Modelo de domínio inicial

```text
User
 │
 ├── Account
 │      └── Transaction (referência por AccountId)
 │
 ├── Category
 └── ...
```

`Account` e `Transaction` são agregados separados.

## Dinheiro

`Money` contém:

```text
amountInMinor: Long
currency: CurrencyCode
```

Exemplo:

```text
R$ 18,90 → Money(1890, BRL)
```

Operações entre moedas diferentes são rejeitadas pelo próprio tipo.

## Saldo

A fonte de verdade será baseada no saldo inicial e movimentos válidos:

```text
saldo atual = saldo inicial + créditos válidos - débitos válidos
```

Caches/projeções poderão existir posteriormente por performance, mas não substituirão os movimentos financeiros como fonte primária.

## Category

`Category` suporta categorias do sistema e personalizadas. Categorias arquivadas permanecem válidas para o histórico, mas não podem receber novas transações.

```text
Category
├── CategoryId
├── ownerId?
├── name
├── type
├── parentId?
├── iconKey?
├── sortOrder
├── status
└── timestamps
```
