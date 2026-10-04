package com.nexa.finance

data class ServerSettings(
    val host: String,
    val port: Int,
) {
    companion object {
        fun fromEnvironment(environment: Map<String, String> = System.getenv()): ServerSettings {
            val host = environment["SERVER_HOST"]?.trim().takeIf { !it.isNullOrEmpty() } ?: "0.0.0.0"
            val port = environment["SERVER_PORT"]?.trim()?.toIntOrNull() ?: 8080
            require(port in 1..65_535) { "SERVER_PORT must be between 1 and 65535." }

            return ServerSettings(
                host = host,
                port = port,
            )
        }
    }
}
