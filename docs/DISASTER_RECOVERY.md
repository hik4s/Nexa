# NEXA Finance — Disaster Recovery Runbook

## Objetivo

Restaurar um backup PostgreSQL de produção em um ambiente isolado, sem modificar a produção, e produzir evidência de integridade antes de considerar a recuperação operacional válida.

## Segurança

- Nunca executar o restore apontando para o PostgreSQL de produção.
- Usar somente referências de variáveis do Railway Bucket; nunca versionar access key, secret, senha ou URL privada.
- O runner exige `DR_ENV=isolated` e encerra com erro em qualquer outro valor.
- Remover o runtime isolado ao final do exercício.

## Componentes versionados

- `ops/postgres-backup/` — geração e verificação dos dumps.
- `ops/postgres-restore/` — imagem e runner de restauração isolada.
- `scripts/restore-postgres.ps1` — restauração local para ambientes de desenvolvimento/teste.

## Procedimento operacional

1. Confirmar um backup real no Bucket privado em `postgres/production/`.
2. Provisionar um PostgreSQL temporário no mesmo projeto, sem volume persistente, com database e usuário dedicados.
3. Configurar apenas rede privada para o PostgreSQL isolado.
4. Provisionar um runtime temporário usando `ops/postgres-restore/`.
5. Configurar o runner com:
   - `DR_ENV=isolated`;
   - `BACKUP_KEY` apontando para o objeto de backup escolhido;
   - `S3_ENDPOINT`, `S3_BUCKET`, `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY` e `AWS_DEFAULT_REGION` via referências do Bucket;
   - `PGHOST`, `PGPORT`, `PGUSER`, `PGDATABASE` e `PGPASSWORD` apontando exclusivamente para o PostgreSQL isolado.
6. Executar o runner e aguardar `DRILL_STATUS=SUCCESS`.
7. Registrar tamanho do dump, entradas de TOC, tempo de download, tempo de `pg_restore`, tempo total do workflow, quantidade de tabelas/índices e validações de constraints/Flyway.
8. Registrar a idade do backup no início do drill como exposição observada de RPO.
9. Excluir imediatamente o runtime isolado e o PostgreSQL temporário.
10. Revalidar produção no Railway antes de encerrar o exercício.

## Evidência do drill de 2026-10-08

Backup usado: `postgres/production/nexa_finance_20261008T030418Z.dump`.

Resultado observado:
- tamanho: 77.815 bytes;
- 175 entradas de TOC;
- download concluído com sucesso;
- `pg_restore`: 2 s;
- workflow completo: 2 s;
- 23 tabelas;
- 74 índices;
- 0 constraints não validadas;
- 0 índices inválidos/não prontos;
- Flyway presente;
- 18 migrações bem-sucedidas;
- 0 migrações com falha;
- versão mais recente: 18;
- users=1;
- accounts=0;
- transactions=0;
- audit_events=0;
- sync_changes=0.

O backup foi concluído às 03:04:18Z e o drill começou às 11:36:05Z. A exposição observada no exercício foi de 8h31m47s entre o ponto de backup e o início da recuperação. A agenda nominal atual é diária às 03:00 UTC, portanto a janela máxima nominal de perda de dados permanece próxima de 24 horas quando o backup diário está disponível.

O RTO medido de 2 s representa apenas este dump de 77,8 KB e o runtime isolado usado no exercício. Não é um RTO de produção garantido.

## Critério de sucesso

O item 3.1 pode ser considerado concluído quando:
- o backup real é identificado;
- o restore ocorre isoladamente;
- a produção permanece inalterada;
- schema, constraints, índices e histórico de migrações estão íntegros;
- o resultado fica registrado;
- o procedimento pode ser repetido usando os artefatos versionados.
