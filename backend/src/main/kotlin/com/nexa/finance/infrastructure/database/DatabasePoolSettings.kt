package com.nexa.finance.infrastructure.database

data class DatabasePoolSettings(
    val maximumPoolSize: Int,
    val minimumIdle: Int,
) {
    companion object {
        fun fromEnvironment(environment: Map<String, String> = System.getenv()): DatabasePoolSettings {
            val maximumPoolSize = environment["POSTGRES_POOL_MAX_SIZE"]
                ?.trim()
                ?.toIntOrNull()
                ?: 10
            require(maximumPoolSize in 1..100) {
                "POSTGRES_POOL_MAX_SIZE must be between 1 and 100."
            }

            val minimumIdle = environment["POSTGRES_POOL_MIN_IDLE"]
                ?.trim()
                ?.toIntOrNull()
                ?: minOf(2, maximumPoolSize)
            require(minimumIdle in 0..maximumPoolSize) {
                "POSTGRES_POOL_MIN_IDLE must be between 0 and POSTGRES_POOL_MAX_SIZE."
            }

            return DatabasePoolSettings(
                maximumPoolSize = maximumPoolSize,
                minimumIdle = minimumIdle,
            )
        }
    }
}
