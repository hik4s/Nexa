package com.nexa.finance

import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

data class RateLimitSettings(
    val requestsPerMinute: Int,
    val refillPeriod: Duration,
) {
    companion object {
        private const val DEFAULT_REQUESTS_PER_MINUTE = 120
        private const val DEFAULT_WINDOW_SECONDS = 60
        private const val MAX_REQUESTS_PER_MINUTE = 10_000
        private const val MAX_WINDOW_SECONDS = 3_600

        fun fromEnvironment(environment: Map<String, String> = System.getenv()): RateLimitSettings {
            val requestsPerMinute = environment["RATE_LIMIT_REQUESTS_PER_MINUTE"]
                ?.trim()
                ?.toIntOrNull()
                ?: DEFAULT_REQUESTS_PER_MINUTE
            require(requestsPerMinute in 1..MAX_REQUESTS_PER_MINUTE) {
                "RATE_LIMIT_REQUESTS_PER_MINUTE must be between 1 and $MAX_REQUESTS_PER_MINUTE."
            }

            val windowSeconds = environment["RATE_LIMIT_WINDOW_SECONDS"]
                ?.trim()
                ?.toLongOrNull()
                ?: DEFAULT_WINDOW_SECONDS.toLong()
            require(windowSeconds in 1..MAX_WINDOW_SECONDS) {
                "RATE_LIMIT_WINDOW_SECONDS must be between 1 and $MAX_WINDOW_SECONDS."
            }

            return RateLimitSettings(
                requestsPerMinute = requestsPerMinute,
                refillPeriod = windowSeconds.seconds,
            )
        }
    }
}
