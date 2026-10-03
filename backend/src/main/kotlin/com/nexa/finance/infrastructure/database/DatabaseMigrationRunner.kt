package com.nexa.finance.infrastructure.database

import org.flywaydb.core.Flyway

object DatabaseMigrationRunner {
    fun migrate(settings: DatabaseSettings = DatabaseSettings.fromEnvironment()) {
        Flyway.configure()
            .dataSource(settings.jdbcUrl, settings.username, settings.password)
            .locations("classpath:db/migration")
            .validateMigrationNaming(true)
            .load()
            .migrate()
    }
}

fun main() {
    DatabaseMigrationRunner.migrate()
}
