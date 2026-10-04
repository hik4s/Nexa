#!/bin/sh
set -eu

exec java -cp "/opt/nexa/lib/*" com.nexa.finance.infrastructure.database.DatabaseMigrationRunnerKt
