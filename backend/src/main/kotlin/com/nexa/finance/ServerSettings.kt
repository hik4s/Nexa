package com.nexa.finance

data class ServerSettings(
    val host: String,
    val port: Int,
    val requestTimeoutMillis: Long,
) {
    companion object {
        fun fromEnvironment(environment: Map<String, String> = System.getenv()): ServerSettings {
            val host = environment["SERVER_HOST"]?.trim().takeIf { !it.isNullOrEmpty() } ?: "0.0.0.0"
            val port = environment["SERVER_PORT"]?.trim()?.toIntOrNull() ?: 8080
            require(port in 1..65_535) { "SERVER_PORT must be between 1 and 65535." }
            val requestTimeoutMillis = environment["SERVER_REQUEST_TIMEOUT_MS"]?.trim()?.toLongOrNull() ?: 15_000L
            require(requestTimeoutMillis in 1_000L..120_000L) {
                "SERVER_REQUEST_TIMEOUT_MS must be between 1000 and 120000."
            }

            return ServerSettings(
                host = host,
                port = port,
                requestTimeoutMillis = requestTimeoutMillis,
            )
        }
    }
}
