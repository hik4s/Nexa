package com.nexa.finance.infrastructure.database

data class DatabaseSettings(
    val jdbcUrl: String,
    val username: String,
    val password: String,
) {
    companion object {
        fun fromEnvironment(environment: Map<String, String> = System.getenv()): DatabaseSettings {
            val jdbcUrl = environment["POSTGRES_JDBC_URL"]?.trim()?.takeIf { it.isNotEmpty() }
                ?: run {
                    val host = environment["POSTGRES_HOST"]?.trim().takeIf { !it.isNullOrEmpty() } ?: "localhost"
                    val port = environment["POSTGRES_PORT"]?.trim().takeIf { !it.isNullOrEmpty() } ?: "5432"
                    val database = environment["POSTGRES_DB"]?.trim().takeIf { !it.isNullOrEmpty() } ?: "nexa_finance"
                    "jdbc:postgresql://$host:$port/$database"
                }
            val username = environment["POSTGRES_USER"]?.trim().takeIf { !it.isNullOrEmpty() } ?: "nexa"
            val password = environment["POSTGRES_PASSWORD"]
                ?: error("POSTGRES_PASSWORD must be set for database migrations.")

            return DatabaseSettings(
                jdbcUrl = jdbcUrl,
                username = username,
                password = password,
            )
        }
    }
}
