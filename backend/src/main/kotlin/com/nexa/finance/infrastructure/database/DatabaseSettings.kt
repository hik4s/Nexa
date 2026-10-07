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
            val passwordFile = environment["POSTGRES_PASSWORD_FILE"]?.trim().takeIf { !it.isNullOrEmpty() }
            val passwordValue = environment["POSTGRES_PASSWORD"]

            val password = when {
                passwordFile != null && passwordValue != null ->
                    error("Set only one of POSTGRES_PASSWORD or POSTGRES_PASSWORD_FILE.")
                passwordFile != null ->
                    java.nio.file.Files.readString(java.nio.file.Path.of(passwordFile)).trimEnd('\r', '\n')
                passwordValue != null -> passwordValue
                else -> error("POSTGRES_PASSWORD or POSTGRES_PASSWORD_FILE must be set for database migrations.")
            }
            require(password.isNotEmpty()) { "Database password must not be empty." }

            return DatabaseSettings(
                jdbcUrl = jdbcUrl,
                username = username,
                password = password,
            )
        }
    }
}
