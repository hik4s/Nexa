# NEXA Finance — Automação de Desenvolvimento

A integração de desenvolvimento do NEXA usa o Git local + GitHub + GitHub Actions. O projeto já está conectado ao repositório privado `hik4s/Nexa`.

## Desenvolvimento local

Na raiz:

```powershell
cd C:\Users\hikas\Downloads\Nexa
Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass
.\scripts\install-git-hooks.ps1
```

Validação rápida:

```powershell
.\scripts\verify.ps1 -Fast -SkipAndroid -SkipInfra
```

Validação completa:

```powershell
.\scripts\verify.ps1
```

Durante desenvolvimento do domínio:

```powershell
.\scripts\watch-backend-tests.ps1
```

## GitHub Actions

Cada push para `main` e cada Pull Request executa automaticamente:

- testes do Backend;
- build do Backend;
- preparação do Android SDK;
- build de debug do Android.

O workflow está em `.github/workflows/ci.yml`.

## Fluxo oficial

```text
Alteração
   ↓
Teste local
   ↓
Commit
   ↓
Push
   ↓
GitHub Actions
   ↓
PASS / FAIL
```

Nenhuma microtarefa deve ser considerada concluída enquanto os testes locais e o CI não estiverem verdes.
