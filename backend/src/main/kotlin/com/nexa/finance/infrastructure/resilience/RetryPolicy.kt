package com.nexa.finance.infrastructure.resilience

import com.nexa.finance.RetrySettings

class RetryPolicy(
    private val settings: RetrySettings,
    private val sleeper: (Long) -> Unit = { millis ->
        if (millis > 0) Thread.sleep(millis)
    },
) {
    fun <T> execute(
        retryable: (Throwable) -> Boolean,
        operation: () -> T,
    ): T {
        var attempt = 1

        while (true) {
            try {
                return operation()
            } catch (error: Throwable) {
                val shouldRetry = attempt < settings.maxAttempts && retryable(error)
                if (!shouldRetry) throw error

                sleeper(backoffForRetry(attempt))
                attempt += 1
            }
        }
    }

    private fun backoffForRetry(attempt: Int): Long {
        if (settings.initialBackoffMillis == 0L) return 0L

        val multiplier = 1L shl minOf(attempt - 1, 30)
        val candidate = settings.initialBackoffMillis * multiplier
        return minOf(settings.maxBackoffMillis, candidate)
    }
}
