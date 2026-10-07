package com.nexa.finance

import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ObservabilityHttpTest {
    @Test
    fun healthReturnsRequestCorrelationId() = testApplication {
        application {
            module(
                observabilitySettings = ObservabilitySettings(
                    metricsEnabled = false,
                    metricsToken = null,
                ),
            )
        }

        val response = client.get("/health")

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("NEXA Finance API - OK", response.bodyAsText())
        val requestId = response.headers[HttpHeaders.XRequestId]
        assertTrue(!requestId.isNullOrBlank())
        assertTrue(requestId.length in 16..128)
    }

    @Test
    fun metricsRequireExplicitEnablementAndToken() = testApplication {
        application {
            module(
                observabilitySettings = ObservabilitySettings(
                    metricsEnabled = true,
                    metricsToken = "scrape-secret",
                ),
            )
        }

        assertEquals(HttpStatusCode.Unauthorized, client.get("/metrics").status)

        val response = client.get("/metrics") {
            header("X-Metrics-Token", "scrape-secret")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.bodyAsText()
        assertTrue(body.contains("ktor_http_server_requests"))
        assertTrue(body.contains("jvm_memory"))
    }

    @Test
    fun metricsRemainUnavailableWhenDisabled() = testApplication {
        application {
            module(
                observabilitySettings = ObservabilitySettings(
                    metricsEnabled = false,
                    metricsToken = "unused",
                ),
            )
        }

        assertEquals(HttpStatusCode.NotFound, client.get("/metrics").status)
    }
}
