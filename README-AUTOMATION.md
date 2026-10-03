# NEXA Finance — Automação de Desenvolvimento

Aplicar este patch uma única vez na cópia atual do projeto.

Depois:

```powershell
cd C:\Users\hikas\Downloads\Nexa
Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass
.\scripts\install-git-hooks.ps1
.\scripts\verify.ps1 -Fast -SkipAndroid -SkipInfra
```

Para testar continuamente durante o desenvolvimento:

```powershell
.\scripts\watch-backend-tests.ps1
```

O workflow do GitHub em `.github/workflows/ci.yml` fica pronto para quando o NEXA tiver um repositório remoto.
