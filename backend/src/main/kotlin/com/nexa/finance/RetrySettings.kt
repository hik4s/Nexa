package com.nexa.finance

data class RetrySettings(
    val maxAttempts: Int,
    val initialBackoffMillis: Long,
    val maxBackoffMillis: Long,
) {
    companion object {
        fun fromEnvironment(environment: Map<String, String> = System.getenv()): RetrySettings {
            val maxAttempts = environment["RETRY_MAX_ATTEMPTS"]?.trim()?.toIntOrNull() ?: 3
            val initialBackoffMillis = environment["RETRY_INITIAL_BACKOFF_MS"]?.trim()?.toLongOrNull() ?: 250L
            val maxBackoffMillis = environment["RETRY_MAX_BACKOFF_MS"]?.trim()?.toLongOrNull() ?: 2_000L

            require(maxAttempts in 1..5) {
                "RETRY_MAX_ATTEMPTS must be between 1 and 5."
            }
            require(initialBackoffMillis in 0L..10_000L) {
                "RETRY_INITIAL_BACKOFF_MS must be between 0 and 10000."
            }
            require(maxBackoffMillis in 0L..30_000L) {
                "RETRY_MAX_BACKOFF_MS must be between 0 and 30000."
            }
            require(maxBackoffMillis >= initialBackoffMillis) {
                "RETRY_MAX_BACKOFF_MS must be greater than or equal to RETRY_INITIAL_BACKOFF_MS."
            }

            return RetrySettings(
                maxAttempts = maxAttempts,
                initialBackoffMillis = initialBackoffMillis,
                maxBackoffMillis = maxBackoffMillis,
            )
        }
    }
}
