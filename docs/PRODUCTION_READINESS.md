# NEXA Finance — Production Readiness

Data de referencia: 2026-10-04

## O que ja esta fechado

- Parte 1 — fundacao tecnica, dominio financeiro e API base.
- Parte 2 — produto completo: registro rapido, dashboard, contas, cartoes/faturas, orcamentos, metas, recorrencias, historico, IA e notificacoes.
- Backend publicado em producao com HTTPS, migrations e rate limiting.
- CI remoto para backend e Android.
- Android local-first com Room, Outbox, checkpoint e WorkManager.
- Sincronizacao real servidor/dispositivo para transacoes.
- Idempotencia de transacoes por UUID.
- Protecao contra conflito de mesmo ID com payload divergente.
- Timeout de leitura/escrita do runtime configuravel por ambiente.
- Paginacao do endpoint de transacoes.
- Scripts locais de backup e restore do PostgreSQL.

## O que pode ser finalizado somente com codigo e ambiente local

1. Refinar observabilidade: logs estruturados, metricas e health/readiness.
2. Criar testes de carga e concorrencia seguros para ambiente local/CI.
3. Fazer benchmark do Android com milhares de transacoes.
4. Completar exportacao de dados do usuario.
5. Refinar hardening transversal dos endpoints e politicas de sessao.
6. Fechar o modelo de versao/revisao para edicoes de transacoes quando a API de edicao for exposta.

## Dependencias externas reais

### Open Finance

A conexao bancaria real exige a escolha de um participante/provedor, onboarding, credenciais e validacao contratual/regulatoria. O codigo pode preparar o adapter e o modelo de consentimento, mas nao deve simular uma conexao bancaria como se estivesse pronta.

### Backups de producao e desastre

Os scripts locais validam a cadeia de backup/restore. A politica de producao depende do provedor de banco, armazenamento externo, retencao, criptografia, RPO/RTO e um exercicio real de restauracao.

### Crash reporting e alertas

Precisam de um fornecedor operacional e de credenciais de projeto. O NEXA deve manter uma camada de abstracao para nao acoplar o dominio a um fornecedor.

### Publicacao Android

Assinatura, keystore de release, conta da Play Console, politica de privacidade publicada, termos, ficha da loja e screenshots sao passos de distribuicao e nao podem ser considerados concluidos apenas com o APK debug.

### Compliance

A LGPD precisa de mapeamento formal de dados, finalidade, base legal, retenção, direitos dos titulares e revisão juridica. A implementacao tecnica deve refletir essas decisoes, nao substitui-las.

## Regra de fechamento

Nenhum item externo sera marcado como concluido sem evidencia de execucao real. Itens internos serao encerrados somente apos implementacao, testes, debugging, revisao e integracao.
