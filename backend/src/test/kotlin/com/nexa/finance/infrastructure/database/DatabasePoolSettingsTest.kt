package com.nexa.finance.infrastructure.database

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class DatabasePoolSettingsTest {

    @Test
    fun usesSafeDevelopmentDefaults() {
        val settings = DatabasePoolSettings.fromEnvironment(emptyMap())

        assertEquals(10, settings.maximumPoolSize)
        assertEquals(2, settings.minimumIdle)
    }

    @Test
    fun readsPoolLimitsFromEnvironment() {
        val settings = DatabasePoolSettings.fromEnvironment(
            mapOf(
                "POSTGRES_POOL_MAX_SIZE" to "20",
                "POSTGRES_POOL_MIN_IDLE" to "5",
            )
        )

        assertEquals(20, settings.maximumPoolSize)
        assertEquals(5, settings.minimumIdle)
    }

    @Test
    fun rejectsInvalidMaximumPoolSize() {
        assertFailsWith<IllegalArgumentException> {
            DatabasePoolSettings.fromEnvironment(mapOf("POSTGRES_POOL_MAX_SIZE" to "0"))
        }
    }

    @Test
    fun rejectsMinimumIdleAboveMaximum() {
        assertFailsWith<IllegalArgumentException> {
            DatabasePoolSettings.fromEnvironment(
                mapOf(
                    "POSTGRES_POOL_MAX_SIZE" to "5",
                    "POSTGRES_POOL_MIN_IDLE" to "6",
                )
            )
        }
    }
}
