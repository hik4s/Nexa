package com.nexa.finance.infrastructure.database

import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import org.junit.jupiter.api.Assumptions.assumeTrue

class DatabaseConnectionProviderIntegrationTest {

    @Test
    fun providerEvictsFailedPoolAndRecoversWhenDatabaseReturns() {
        assumeTrue(!System.getenv("POSTGRES_PASSWORD").isNullOrBlank())
        var healthy = false
        val provider = DatabaseConnectionProvider(
            settingsProvider = {
                if (!healthy) {
                    DatabaseSettings(
                        jdbcUrl = "jdbc:postgresql://127.0.0.1:1/nexa_finance_ci",
                        username = "nexa",
                        password = "nexa_ci_password",
                    )
                } else {
                    DatabaseSettings(
                        jdbcUrl = "jdbc:postgresql://127.0.0.1:5432/nexa_finance_ci",
                        username = "nexa",
                        password = "nexa_ci_password",
                    )
                }
            },
            poolSettingsProvider = { DatabasePoolSettings(maximumPoolSize = 1, minimumIdle = 0) },
        )
        try {
            assertFailsWith<Exception> {
                provider.open()
            }
            healthy = true
            provider.open().use { connection ->
                assertTrue(connection.isValid(2))
            }
        } finally {
            provider.close()
        }
    }

    @Test
    fun opensMultipleConnectionsThroughThePool() {
        assumeTrue(!System.getenv("POSTGRES_PASSWORD").isNullOrBlank())
        val provider = DatabaseConnectionProvider()
        try {
            provider.open().use { first ->
                provider.open().use { second ->
                    assertTrue(first.isValid(2))
                    assertTrue(second.isValid(2))
                    assertTrue(first !== second)
                }
            }
        } finally {
            provider.close()
        }
    }
}
