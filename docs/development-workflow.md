# NEXA Finance — Development Workflow

## Objetivo

Eliminar o ciclo de ZIP/extrair/testar entre microtarefas. O repositório Git local é a fonte de trabalho; CI valida cada push/PR.

## Fluxo local

### Verificação rápida

```powershell
.\scripts\verify.ps1 -Fast -SkipAndroid -SkipInfra
```

Executa testes e build do backend.

### Verificação completa

```powershell
.\scripts\verify.ps1
```

Executa backend, Android e validação do Compose.

### Testes contínuos

```powershell
.\scripts\watch-backend-tests.ps1
```

O Gradle reexecuta os testes quando arquivos relevantes mudam.

### Pre-push

```powershell
.\scripts\install-git-hooks.ps1
```

Depois disso, cada `git push` executa a verificação rápida antes de enviar alterações.

## CI

O workflow `.github/workflows/ci.yml` executa testes/build do backend e build do APK Android em cada push e pull request.

## Regra do projeto

Uma microtarefa não entra no próximo checkpoint enquanto:

- os testes locais não passam;
- o build não passa;
- o CI não passa quando o repositório remoto estiver configurado;
- não houver regressão relevante.
