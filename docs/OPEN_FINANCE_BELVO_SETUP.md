# NEXA Finance — Belvo Sandbox Setup

## Objetivo

Habilitar a conexão Open Finance em ambiente de desenvolvimento usando o produto Banking Brazil / OFDA da Belvo.

A configuração fica desativada por padrão no NEXA. Não coloque credenciais do Belvo no repositório, no APK ou em arquivos versionados.

## Variáveis

Configure somente no ambiente do backend:

- `BELVO_ENABLED=true`
- `BELVO_BASE_URL=https://sandbox.belvo.com`
- `BELVO_SECRET_ID=<credencial-do-sandbox>`
- `BELVO_SECRET_PASSWORD=<credencial-do-sandbox>`
- `BELVO_TERMS_URL=https://<dominio-do-nexa>/legal/terms`
- `BELVO_CALLBACK_SUCCESS_URL=nexa://open-finance/success`
- `BELVO_CALLBACK_EXIT_URL=nexa://open-finance/exit`
- `BELVO_CALLBACK_EVENT=nexa://open-finance/error`
- `BELVO_COMPANY_ICON_URL=https://<dominio-do-nexa>/assets/icon.svg`
- `BELVO_COMPANY_LOGO_URL=https://<dominio-do-nexa>/assets/logo.svg`
- `BELVO_COMPANY_NAME=NEXA Finance`
- `BELVO_PURPOSE=Organizar sua vida financeira com segurança.`
- `BELVO_STALE_IN=300d`

## Fluxo

1. Criar o consentimento NEXA.
2. Solicitar `POST /api/v1/open-finance/consents/{id}/authorization-session` informando CPF/CNPJ e nome do titular.
3. O backend chama `POST /api/token/` no sandbox da Belvo.
4. O backend retorna a URL do Hosted Widget com token temporário de 10 minutos.
5. O aplicativo abre a URL na WebView/fluxo App2App e escuta o deep link de sucesso.
6. O callback de sucesso da Belvo entrega o Link ID. O aplicativo associa o Link ID ao consentimento NEXA.
7. O backend aguarda os webhooks da Belvo antes de recuperar dados históricos.
8. Após os webhooks, o backend recupera contas, transações, faturas e demais recursos habilitados e normaliza os dados para o domínio NEXA.

## Requisitos externos ainda pendentes

- Conta Belvo criada e acesso ao Sandbox.
- Credenciais de Sandbox válidas.
- Termos e condições acessíveis por HTTPS.
- Ícone e logotipo publicados por HTTPS.
- Deep links registrados no Android.
- URL de callback compatível com o ambiente.
- Teste ponta a ponta com a instituição Mockbank.
- Definição comercial/jurídica para eventual produção.

## Segurança

O access token do Hosted Widget é temporário e não deve ser persistido pelo backend. O NEXA também não deve armazenar credenciais bancárias do usuário.

O `external_id` enviado no início do widget deve ser um identificador interno não-PII e não deve conter e-mail, nome, telefone ou dados financeiros.

## Referências oficiais

- Belvo OFDA Widget: https://developers.belvo.com/pt-br/products/aggregation_brazil/aggregation-brazil-integration-widget
- Belvo Widget Access Token API: https://developers.belvo.com/pt-br/apis/belvoopenapispec/widget-access-token
- Belvo Links: https://developers.belvo.com/pt-br/apis/belvoopenapispec/links
