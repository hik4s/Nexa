package com.nexa.finance.infrastructure.database

import kotlin.test.Test
import kotlin.test.assertTrue
import org.junit.jupiter.api.Assumptions.assumeTrue

class DatabaseConnectionProviderIntegrationTest {

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
