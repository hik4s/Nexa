#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

required=(
  README.md
  NEXA_FINANCE_MASTER_PLAN.md
  .gitignore
  .editorconfig
  .gitattributes
  .env.example
  android/settings.gradle.kts
  android/build.gradle.kts
  android/app/build.gradle.kts
  backend/settings.gradle.kts
  backend/build.gradle.kts
  infra/docker-compose.yml
  docs/repository.md
  docs/development-workflow.md
  docs/architecture.md
)

for file in "${required[@]}"; do
  [[ -f "$file" ]] || { echo "MISSING: $file" >&2; exit 1; }
done

if git ls-files | grep -E '(^|/)(\.env|\.env\.|local\.properties)$' >/dev/null; then
  echo "ERROR: local secret/config file is tracked" >&2
  exit 1
fi

if grep -RIn --exclude-dir=.git --exclude='*.md' -E 'POSTGRES_PASSWORD[=:][[:space:]]*[^\$\{[:space:]]' infra android backend 2>/dev/null | grep -v '\${POSTGRES_PASSWORD' >/dev/null; then
  echo "ERROR: hardcoded Postgres password detected" >&2
  exit 1
fi

echo "NEXA structure checks: PASS"
