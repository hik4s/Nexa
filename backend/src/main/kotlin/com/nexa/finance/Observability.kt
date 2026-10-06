package com.nexa.finance

import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import java.util.concurrent.atomic.AtomicLong

class NexaObservability(
    val metricsRegistry: PrometheusMeterRegistry = createRegistry(),
) {
    private val readinessFailures = AtomicLong(0)

    fun recordReadinessFailure() {
        readinessFailures.incrementAndGet()
    }

    fun readinessFailureCount(): Long = readinessFailures.get()

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