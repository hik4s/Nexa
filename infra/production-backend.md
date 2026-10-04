# NEXA Finance — backend container de produção

O backend é empacotado como uma distribuição JVM e executado em uma imagem JRE 17 sem privilégios de root.

## Variáveis obrigatórias

O processo usa configuração por ambiente. Para um ambiente de produção, injete os valores no runtime; não grave segredos no repositório nem na imagem:

- SERVER_HOST — normalmente 0.0.0.0.
- SERVER_PORT — normalmente 8080.
- POSTGRES_HOST
- POSTGRES_PORT
- POSTGRES_DB
- POSTGRES_USER
- POSTGRES_PASSWORD ou POSTGRES_PASSWORD_FILE
- POSTGRES_POOL_MAX_SIZE — padrão 10.
- POSTGRES_POOL_MIN_IDLE — padrão 2.

Prefira POSTGRES_PASSWORD_FILE em ambientes que disponibilizam secrets como arquivo montado. A aplicação rejeita a definição simultânea das duas fontes para evitar ambiguidade.

As conexões JDBC usam um pool HikariCP compartilhado pelo backend. Cada processo mantém um único pool e o encerra no desligamento da aplicação. O pool também habilita TCP keepalive para o driver PostgreSQL.

A aplicação também aceita POSTGRES_JDBC_URL quando o ambiente precisar controlar a URL JDBC inteira.

## Build da imagem

A partir de backend/:

```powershell
docker build -t nexa-finance-backend:local .
```

## Execução de validação

```powershell
docker run --rm -p 8080:8080 `
  -e POSTGRES_HOST=host.docker.internal `
  -e POSTGRES_PORT=5432 `
  -e POSTGRES_DB=nexa_finance `
  -e POSTGRES_USER=nexa `
  -e POSTGRES_PASSWORD=<senha-local> `
  nexa-finance-backend:local
```

O endpoint de liveness disponível é GET /health.

Este arquivo não declara um provedor de produção, DNS, TLS ou banco gerenciado. Esses itens permanecem dependentes da infraestrutura de implantação escolhida e não devem ser marcados como concluídos sem validação real.
