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

class TransactionApiTest {
    @Test
    fun transactionApiCreatesListsAndEnforcesIsolationAgainstPostgres() = testApplication {
        assumeTrue(hasPostgres())
        val ownerEmail = uniqueEmail("tx-owner")
        val otherEmail = uniqueEmail("tx-other")
        application { module() }
        try {
            val ownerToken = registerAndLogin(client, ownerEmail, "owner-password", "Owner")
            val otherToken = registerAndLogin(client, otherEmail, "other-password", "Other")
            val account = client.post("/api/v1/accounts") {
                bearer(ownerToken)
                jsonBody("""{"name":"Transaction Account","type":"CHECKING","openingBalanceMinor":50000,"currency":"BRL"}""")
            }
            assertEquals(201, account.status.value)
            val accountId = json(account.bodyAsText())["id"]!!.jsonPrimitive.content
            val create = client.post("/api/v1/transactions") {
                bearer(ownerToken)
                jsonBody("""{"accountId":"$accountId","amountMinor":12500,"currency":"BRL","type":"EXPENSE","direction":"DEBIT","occurredOn":"2026-10-03","description":"Mercado","source":"MANUAL"}""")
            }
            assertEquals(201, create.status.value, create.bodyAsText())
            val transactionId = json(create.bodyAsText())["id"]!!.jsonPrimitive.content
            assertTrue(create.bodyAsText().contains(""""status":"POSTED""""))
            val ownerList = client.get("/api/v1/transactions?accountId=$accountId") { bearer(ownerToken) }
            assertEquals(200, ownerList.status.value)
            assertTrue(ownerList.bodyAsText().contains(transactionId))
            val otherList = client.get("/api/v1/transactions?accountId=$accountId") { bearer(otherToken) }
            assertEquals(200, otherList.status.value)
            assertFalse(otherList.bodyAsText().contains(transactionId))
            assertEquals(400, client.get("/api/v1/transactions/$transactionId") { bearer(otherToken) }.status.value)
            assertEquals(400, client.post("/api/v1/transactions/$transactionId/cancel") { bearer(otherToken) }.status.value)
            val balance = client.get("/api/v1/accounts/$accountId") { bearer(ownerToken) }
            assertEquals(200, balance.status.value, balance.bodyAsText())
            assertTrue(balance.bodyAsText().contains(""""balanceMinor":37500"""), balance.bodyAsText())
        } finally {
            cleanupUsers(ownerEmail, otherEmail)
        }
    }

    @Test
    fun transactionApiRejectsCurrencyMismatchAgainstPostgres() = testApplication {
        assumeTrue(hasPostgres())
        val email = uniqueEmail("tx-validation")
        application { module() }
        try {
            val token = registerAndLogin(client, email, "password-123", "Validation User")
            val account = client.post("/api/v1/accounts") {
                bearer(token)
                jsonBody("""{"name":"USD Account","type":"CHECKING","openingBalanceMinor":1000,"currency":"USD"}""")
            }
            assertEquals(201, account.status.value)
            val accountId = json(account.bodyAsText())["id"]!!.jsonPrimitive.content
            val mismatch = client.post("/api/v1/transactions") {
                bearer(token)
                jsonBody("""{"accountId":"$accountId","amountMinor":100,"currency":"BRL","type":"EXPENSE","direction":"DEBIT","occurredOn":"2026-10-03"}""")
            }
            assertEquals(400, mismatch.status.value)
        } finally {
            cleanupUsers(email)
        }
    }

    @Test
    fun authenticationApiLifecycleWorksAgainstPostgres() = testApplication {
        assumeTrue(hasPostgres())
        val email = uniqueEmail("api")
        application { module() }
        try {
            val token = registerAndLogin(client, email, "api-password", "API User")
            assertEquals(200, client.get("/api/v1/auth/me") { bearer(token) }.status.value)
            assertEquals(204, client.post("/api/v1/auth/logout") { bearer(token) }.status.value)
            assertEquals(401, client.get("/api/v1/auth/me") { bearer(token) }.status.value)
        } finally { cleanupUsers(email) }
    }

    @Test
    fun healthEndpointReturnsOk() = testApplication {
        application { module() }
        val response = client.get("/health")
        assertEquals(200, response.status.value)
        assertEquals("NEXA Finance API - OK", response.bodyAsText().trim())
    }

    private suspend fun registerAndLogin(client: HttpClient, email: String, password: String, displayName: String): String {
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

    private fun io.ktor.client.request.HttpRequestBuilder.bearer(token: String) {
        header(HttpHeaders.Authorization, "Bearer $token")
    }

    private fun json(body: String) = Json.parseToJsonElement(body).jsonObject
    private fun uniqueEmail(prefix: String): String = prefix + "-" + UUID.randomUUID() + "@example.com"
    private fun hasPostgres(): Boolean = !System.getenv("POSTGRES_PASSWORD").isNullOrBlank()

    private fun cleanupUsers(vararg emails: String) {
        DatabaseConnectionProvider().open().use { connection ->
            connection.autoCommit = false
            try {
                connection.prepareStatement("DELETE FROM transactions WHERE account_id IN (SELECT a.id FROM accounts a JOIN users u ON u.id = a.owner_id WHERE u.email = ANY (?))").use { statement ->
                    statement.setArray(1, connection.createArrayOf("text", emails))
                    statement.executeUpdate()
                }
                connection.prepareStatement("DELETE FROM accounts WHERE owner_id IN (SELECT id FROM users WHERE email = ANY (?))").use { statement ->
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