package com.nexa.finance.infrastructure.resilience

import com.nexa.finance.RetrySettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class RetryPolicyTest {
    @Test
    fun retriesOnlyWhenPredicateAllowsAndUsesExponentialBackoff() {
        val delays = mutableListOf<Long>()
        val policy = RetryPolicy(
            settings = RetrySettings(
                maxAttempts = 4,
                initialBackoffMillis = 100,
                maxBackoffMillis = 250,
            ),
            sleeper = delays::add,
        )
        var attempts = 0

        val result = policy.execute(
            retryable = { it is IllegalStateException },
        ) {
            attempts += 1
            if (attempts < 4) error("transient")
            "ok"
        }

        assertEquals("ok", result)
        assertEquals(4, attempts)
        assertEquals(listOf(100L, 200L, 250L), delays)
    }

    @Test
    fun permanentFailureIsNotRetried() {
        val delays = mutableListOf<Long>()
        val policy = RetryPolicy(
            settings = RetrySettings(
                maxAttempts = 5,
                initialBackoffMillis = 100,
                maxBackoffMillis = 1_000,
            ),
            sleeper = delays::add,
        )
        var attempts = 0

        assertFailsWith<IllegalArgumentException> {
            policy.execute(retryable = { false }) {
                attempts += 1
                throw IllegalArgumentException("permanent")
            }
        }

        assertEquals(1, attempts)
        assertTrue(delays.isEmpty())
    }

    @Test
    fun retryStopsAtMaximumAttempts() {
        val delays = mutableListOf<Long>()
        val policy = RetryPolicy(
            settings = RetrySettings(
                maxAttempts = 3,
                initialBackoffMillis = 10,
                maxBackoffMillis = 100,
            ),
            sleeper = delays::add,
        )
        var attempts = 0

        assertFailsWith<IllegalStateException> {
            policy.execute(retryable = { true }) {
                attempts += 1
                error("still failing")
            }
        }

        assertEquals(3, attempts)
        assertEquals(listOf(10L, 20L), delays)
    }

    @Test
    fun settingsUseSafeDefaults() {
        assertEquals(
            RetrySettings(
                maxAttempts = 3,
                initialBackoffMillis = 250,
                maxBackoffMillis = 2_000,
            ),
            RetrySettings.fromEnvironment(emptyMap()),
        )
    }

    @Test
    fun settingsRejectUnsafeBounds() {
        assertFailsWith<IllegalArgumentException> {
            RetrySettings.fromEnvironment(mapOf("RETRY_MAX_ATTEMPTS" to "6"))
        }
        assertFailsWith<IllegalArgumentException> {
            RetrySettings.fromEnvironment(mapOf("RETRY_INITIAL_BACKOFF_MS" to "10001"))
        }
        assertFailsWith<IllegalArgumentException> {
            RetrySettings.fromEnvironment(mapOf("RETRY_MAX_BACKOFF_MS" to "100", "RETRY_INITIAL_BACKOFF_MS" to "200"))
        }
    }
}
