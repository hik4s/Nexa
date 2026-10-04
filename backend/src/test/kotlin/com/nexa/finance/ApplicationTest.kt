package com.nexa.finance

import com.nexa.finance.infrastructure.database.DatabaseConnectionProvider
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.jupiter.api.Assumptions.assumeTrue

class ApplicationTest {
    @Test
    fun authenticationApiLifecycleWorksAgainstPostgres() = testApplication {
        assumeTrue(hasPostgres())
        val email = uniqueEmail("api")
        val password = "api-password"

        application { module() }

        try {
            val token = registerAndLogin(client, email, password, "API User")

            val me = client.get("/api/v1/auth/me") {
                header(HttpHeaders.Authorization, "Bearer $token")
            }
            assertEquals(200, me.status.value)
            assertTrue(me.bodyAsText().contains("API User"))

            val logout = client.post("/api/v1/auth/logout") {
                header(HttpHeaders.Authorization, "Bearer $token")
            }
            assertEquals(204, logout.status.value)

            val afterLogout = client.get("/api/v1/auth/me") {
                header(HttpHeaders.Authorization, "Bearer $token")
            }
            assertEquals(401, afterLogout.status.value)
        } finally {
            cleanupUsers(email)
        }
    }

    @Test
    fun recoveryApiResetsPasswordAndInvalidatesExistingSession() = testApplication {
        assumeTrue(hasPostgres())
        val email = uniqueEmail("recovery")
        val oldPassword = "old-password"
        val newPassword = "new-password"

        application { module() }

        try {
            val oldToken = registerAndLogin(client, email, oldPassword, "Recovery User")

            val request = client.post("/api/v1/auth/recovery/request") {
                jsonBody("""{"email":"$email"}""")
            }
            assertEquals(200, request.status.value)
            val recoveryToken = json(request.bodyAsText())["token"]!!.jsonPrimitive.content

            val reset = client.post("/api/v1/auth/recovery/reset") {
                jsonBody("""{"token":"$recoveryToken","newPassword":"$newPassword"}""")
            }
            assertEquals(204, reset.status.value)

            val oldSession = client.get("/api/v1/auth/me") {
                header(HttpHeaders.Authorization, "Bearer $oldToken")
            }
            assertEquals(401, oldSession.status.value)

            val oldLogin = client.post("/api/v1/auth/login") {
                jsonBody("""{"email":"$email","password":"$oldPassword"}""")
            }
            assertEquals(400, oldLogin.status.value)

            val newLogin = client.post("/api/v1/auth/login") {
                jsonBody("""{"email":"$email","password":"$newPassword"}""")
            }
            assertEquals(200, newLogin.status.value)

            val secondReset = client.post("/api/v1/auth/recovery/reset") {
                jsonBody("""{"token":"$recoveryToken","newPassword":"another-password"}""")
            }
            assertEquals(400, secondReset.status.value)
        } finally {
            cleanupUsers(email)
        }
    }

    @Test
    fun profileApiUpdatesFields() = testApplication {
        assumeTrue(hasPostgres())
        val email = uniqueEmail("profile")

        application { module() }

        try {
            val token = registerAndLogin(client, email, "profile-password", "Before")

            val update = client.put("/api/v1/auth/me") {
                header(HttpHeaders.Authorization, "Bearer $token")
                jsonBody("""{"displayName":"After","timeZone":"America/Sao_Paulo"}""")
            }
            assertEquals(200, update.status.value)
            assertTrue(update.bodyAsText().contains("After"))
            assertTrue(update.bodyAsText().contains("America/Sao_Paulo"))

            val me = client.get("/api/v1/auth/me") {
                header(HttpHeaders.Authorization, "Bearer $token")
            }
            assertEquals(200, me.status.value)
            assertTrue(me.bodyAsText().contains("After"))
            assertTrue(me.bodyAsText().contains("America/Sao_Paulo"))
        } finally {
            cleanupUsers(email)
        }
    }

    @Test
    fun accountApiEnforcesUserIsolationAgainstPostgres() = testApplication {
        assumeTrue(hasPostgres())
        val ownerEmail = uniqueEmail("owner")
        val otherEmail = uniqueEmail("other")

        application { module() }

        try {
            val ownerToken = registerAndLogin(client, ownerEmail, "owner-password", "Owner")
            val otherToken = registerAndLogin(client, otherEmail, "other-password", "Other")

            val create = client.post("/api/v1/accounts") {
                header(HttpHeaders.Authorization, "Bearer $ownerToken")
                jsonBody("""{"name":"NEXA Checking","type":"CHECKING","openingBalanceMinor":100000,"currency":"BRL"}""")
            }
            assertEquals(201, create.status.value)
            val accountId = json(create.bodyAsText())["id"]!!.jsonPrimitive.content

            val ownerList = client.get("/api/v1/accounts") {
                header(HttpHeaders.Authorization, "Bearer $ownerToken")
            }
            assertEquals(200, ownerList.status.value)
            assertTrue(ownerList.bodyAsText().contains(accountId))

            val otherList = client.get("/api/v1/accounts") {
                header(HttpHeaders.Authorization, "Bearer $otherToken")
            }
            assertEquals(200, otherList.status.value)
            assertFalse(otherList.bodyAsText().contains(accountId))

            val crossUser = client.get("/api/v1/accounts/$accountId") {
                header(HttpHeaders.Authorization, "Bearer $otherToken")
            }
            assertEquals(400, crossUser.status.value)

            val unauthenticated = client.get("/api/v1/accounts")
            assertEquals(401, unauthenticated.status.value)
        } finally {
            cleanupUsers(ownerEmail, otherEmail)
        }
    }

    @Test
    fun invalidJsonReturnsStandardApiError() = testApplication {
        application { module() }

        val response = client.post("/api/v1/auth/login") {
            jsonBody("""{}""")
        }

        assertEquals(400, response.status.value)
        assertTrue(response.bodyAsText().contains("VALIDATION_ERROR"))
    }

    @Test
    fun healthEndpointReturnsOk() = testApplication {
        application { module() }

        val response = client.get("/health")
        assertEquals(200, response.status.value)
        assertEquals("NEXA Finance API - OK", response.bodyAsText().trim())
    }

    private suspend fun registerAndLogin(
        client: HttpClient,
        email: String,
        password: String,
        displayName: String,
    ): String {
        val register = client.post("/api/v1/auth/register") {
            jsonBody("""{"email":"$email","displayName":"$displayName","password":"$password","timeZone":"UTC"}""")
        }
        assertEquals(201, register.status.value)

        val login = client.post("/api/v1/auth/login") {
            jsonBody("""{"email":"$email","password":"$password"}""")
        }
        assertEquals(200, login.status.value)
        return json(login.bodyAsText())["token"]!!.jsonPrimitive.content
    }

    private fun io.ktor.client.request.HttpRequestBuilder.jsonBody(body: String) {
        header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
        setBody(body)
    }

    private fun json(body: String) = Json.parseToJsonElement(body).jsonObject

    private fun uniqueEmail(prefix: String): String =
        prefix + "-" + UUID.randomUUID() + "@example.com"

    private fun hasPostgres(): Boolean = !System.getenv("POSTGRES_PASSWORD").isNullOrBlank()

    private fun cleanupUsers(vararg emails: String) {
        DatabaseConnectionProvider().open().use { connection ->
            connection.autoCommit = false
            try {
                connection.prepareStatement(
                    "DELETE FROM accounts WHERE owner_id IN (SELECT id FROM users WHERE email = ANY (?))"
                ).use { statement ->
                    statement.setArray(1, connection.createArrayOf("text", emails))
                    statement.executeUpdate()
                }
                connection.prepareStatement("DELETE FROM users WHERE email = ANY (?)").use { statement ->
                    statement.setArray(1, connection.createArrayOf("text", emails))
                    statement.executeUpdate()
                }
                connection.commit()
            } catch (e: Exception) {
                connection.rollback()
                throw e
            }
        }
    }
}
