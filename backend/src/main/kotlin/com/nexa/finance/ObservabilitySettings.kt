package com.nexa.finance

data class ObservabilitySettings(
    val metricsEnabled: Boolean,
    val metricsToken: String?,
) {
    companion object {
        fun fromEnvironment(environment: Map<String, String> = System.getenv()): ObservabilitySettings =
            ObservabilitySettings(
                metricsEnabled = environment["NEXA_METRICS_ENABLED"]?.trim()?.equals("true", ignoreCase = true) == true,
                metricsToken = environment["NEXA_METRICS_TOKEN"]?.trim()?.takeIf { it.isNotEmpty() },
            )
    }
}
