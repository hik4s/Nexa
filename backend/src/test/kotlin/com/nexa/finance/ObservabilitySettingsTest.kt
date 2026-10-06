package com.nexa.finance

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ObservabilitySettingsTest {
    @Test
    fun metricsAreDisabledByDefault() {
        val settings = ObservabilitySettings.fromEnvironment(emptyMap())
        assertFalse(settings.metricsEnabled)
        assertNull(settings.metricsToken)
    }

    @Test
    fun metricsCanBeEnabledWithDedicatedToken() {
        val settings = ObservabilitySettings.fromEnvironment(
            mapOf(
                "NEXA_METRICS_ENABLED" to "true",
                "NEXA_METRICS_TOKEN" to "scrape-secret",
            ),
        )
        assertTrue(settings.metricsEnabled)
        assertTrue(settings.metricsToken == "scrape-secret")
    }
}
