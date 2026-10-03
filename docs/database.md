# NEXA Finance — Banco de dados e migrations

## Stack

- PostgreSQL 18 para desenvolvimento local
- Flyway para migrations versionadas
- PostgreSQL JDBC para conexão do runner
- Exposed será introduzido depois, quando a camada de persistência for implementada

## Convenções

- Tabelas e colunas usam `snake_case`.
- IDs são `UUID`.
- Valores monetários são inteiros em unidades mínimas da moeda (`*_minor`).
- Datas de negócio usam `DATE`.
- Timestamps de auditoria usam `TIMESTAMPTZ`.
- Estados e tipos do domínio são armazenados como `VARCHAR` com `CHECK` explícito.
- Foreign keys usam nomes `fk_<tabela>_<referencia>`.
- Índices usam `idx_<tabela>_<colunas>`.

## Migration atual

`V1__initial_schema.sql` cria os agregados financeiros já definidos no domínio: accounts, categories, transactions, transfers, installments, installment_items e recurrences.

`owner_id` e `user_id` ainda não possuem FK para uma tabela de usuários. Essa relação será criada quando o agregado `User` e a autenticação forem formalizados na Parte 1.7.

## Executando localmente

Com PostgreSQL em execução e `.env` configurado:

    .\scripts\migrate.ps1

O runner lê o `.env` apenas para o processo atual e executa o task Gradle `:migrateDatabase`.

## Verificação

A primeira execução deve aplicar a versão 1. Uma segunda execução deve validar a migration e informar que o schema já está atualizado.

Para confirmar rapidamente no PostgreSQL:

    docker exec nexa-finance-postgres psql -U nexa -d nexa_finance -c "SELECT version, description, success FROM flyway_schema_history ORDER BY installed_rank;"

## Criação do banco do zero

O cenário local descartável pode ser reproduzido com:

    docker compose --env-file .env -f .\infra\docker-compose.yml down -v
    docker compose --env-file .env -f .\infra\docker-compose.yml up -d
    .\scripts\migrate.ps1

Isso valida que o banco pode ser reconstruído apenas pela infraestrutura + migrations versionadas.

## Rollback

Não são utilizadas migrations `down` para o schema financeiro. A estratégia será evolutiva: mudanças destrutivas ou reversões serão feitas por novas migrations versionadas, preservando o histórico aplicado. O rollback físico do banco local descartável é feito recriando o volume.
