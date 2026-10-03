# NEXA Finance — Decisões de migrations

## Flyway

Flyway foi escolhido porque o projeto já definiu migrations SQL versionadas e precisa que a evolução do schema permaneça explícita e revisável.

## Runner separado

As migrations não são executadas automaticamente no `Application.module()`. O backend possui um task Gradle dedicado, `:migrateDatabase`, e o script `scripts/migrate.ps1` carrega a configuração local.

Isso evita acoplar o boot da API a uma operação potencialmente destrutiva de schema e deixa a execução explícita em desenvolvimento e CI.

## IDs de usuário

Os agregados financeiros já usam `UserId`, mas o agregado User ainda não foi modelado. Por isso `owner_id` e `user_id` são UUIDs indexados sem FK na V1. A FK será adicionada em uma migration posterior quando a tabela de usuários existir.
