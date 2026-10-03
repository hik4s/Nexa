package com.nexa.finance

import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals

class ApplicationTest {
    @Test
    fun healthEndpointReturnsOk() = testApplication {
        application { module() }

        val response = client.get("/health")
        assertEquals(200, response.status.value)
        assertEquals("NEXA Finance API — OK", response.bodyAsText())
    }
}
