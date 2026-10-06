package com.nexa.finance

import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import io.micrometer.core.instrument.Counter

class NexaObservability(
    val metricsRegistry: PrometheusMeterRegistry = createRegistry(),
) {
    private val readinessFailures: Counter by lazy {
        Counter.builder("nexa.readiness.failures")
            .description("Number of readiness probes that could not reach PostgreSQL")
            .register(metricsRegistry)
    }

    fun recordReadinessFailure() {
        readinessFailures.increment()
    }

    fun readinessFailureCount(): Double = readinessFailures.count()

    companion object {
        fun createRegistry(): PrometheusMeterRegistry =
            PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
    }
}

fun validateMetricsToken(expected: String?, provided: String?): Boolean {
    if (expected.isNullOrBlank() || provided.isNullOrBlank()) return false
    return java.security.MessageDigest.isEqual(
        expected.toByteArray(Charsets.UTF_8),
        provided.toByteArray(Charsets.UTF_8),
    )
}