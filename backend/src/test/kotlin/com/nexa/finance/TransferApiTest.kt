package com.nexa.finance

import com.nexa.finance.infrastructure.database.DatabaseConnectionProvider
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Assumptions.assumeTrue
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TransferApiTest {
    @Test
    fun transferApiCreatesPostsAndIsolatesUsersAgainstPostgres() = testApplication {
        assumeTrue(hasPostgres())
        val ownerEmail = uniqueEmail("transfer-owner")
        val otherEmail = uniqueEmail("transfer-other")
        application { module() }
        try {
            assertEquals(401, client.get("/api/v1/transfers").status.value)
            val ownerToken = registerAndLogin(client, ownerEmail, "owner-password", "Owner")
            val otherToken = registerAndLogin(client, otherEmail, "other-password", "Other")
            val sourceAccountId = createAccount(client, ownerToken, "Source", 50_000)
            val destinationAccountId = createAccount(client, ownerToken, "Destination", 50_000)

            val create = client.post("/api/v1/transfers") {
                bearer(ownerToken)
                jsonBody("""{"sourceAccountId":"$sourceAccountId","destinationAccountId":"$destinationAccountId","amountMinor":10000,"currency":"BRL","occurredOn":"2026-10-03","source":"MANUAL"}""")
            }
            assertEquals(201, create.status.value, create.bodyAsText())
            val transferId = json(create.bodyAsText())["id"]!!.jsonPrimitive.content
            assertTrue(create.bodyAsText().contains(""""status":"PENDING""""), create.bodyAsText())

            val list = client.get("/api/v1/transfers") { bearer(ownerToken) }
            assertEquals(200, list.status.value)
            assertTrue(list.bodyAsText().contains(transferId))

            val otherList = client.get("/api/v1/transfers") { bearer(otherToken) }
            assertEquals(200, otherList.status.value)
            assertFalse(otherList.bodyAsText().contains(transferId))
            assertEquals(400, client.get("/api/v1/transfers/$transferId") { bearer(otherToken) }.status.value)
            assertEquals(400, client.post("/api/v1/transfers/$transferId/post") { bearer(otherToken) }.status.value)

            val posted = client.post("/api/v1/transfers/$transferId/post") { bearer(ownerToken) }
            assertEquals(200, posted.status.value, posted.bodyAsText())
            assertTrue(posted.bodyAsText().contains(""""status":"POSTED""""), posted.bodyAsText())

            val source = client.get("/api/v1/accounts/$sourceAccountId") { bearer(ownerToken) }
            val destination = client.get("/api/v1/accounts/$destinationAccountId") { bearer(ownerToken) }
            assertEquals(200, source.status.value)
            assertEquals(200, destination.status.value)
            assertEquals(40000L, json(source.bodyAsText())["balanceMinor"]!!.jsonPrimitive.content.toLong())
            assertEquals(60000L, json(destination.bodyAsText())["balanceMinor"]!!.jsonPrimitive.content.toLong())

            assertEquals(400, client.post("/api/v1/transfers/$transferId/cancel") { bearer(ownerToken) }.status.value)
        } finally {
            cleanupUser(ownerEmail)
            cleanupUser(otherEmail)
        }
    }

    @Test
    fun transferApiRejectsInvalidReferencesAndCurrency() = testApplication {
        assumeTrue(hasPostgres())
        val email = uniqueEmail("transfer-validation")
        application { module() }
        try {
            val token = registerAndLogin(client, email, "password-123", "Validation")
            val source = createAccount(client, token, "Source", 10_000)
            val destination = createAccount(client, token, "Destination", 10_000)

            val sameAccount = client.post("/api/v1/transfers") {
                bearer(token)
                jsonBody("""{"sourceAccountId":"$source","destinationAccountId":"$source","amountMinor":1000,"currency":"BRL","occurredOn":"2026-10-03"}""")
            }
            assertEquals(400, sameAccount.status.value)

            val mismatch = client.post("/api/v1/transfers") {
                bearer(token)
                jsonBody("""{"sourceAccountId":"$source","destinationAccountId":"$destination","amountMinor":1000,"currency":"USD","occurredOn":"2026-10-03"}""")
            }
            assertEquals(400, mismatch.status.value)

            val invalidAmount = client.post("/api/v1/transfers") {
                bearer(token)
                jsonBody("""{"sourceAccountId":"$source","destinationAccountId":"$destination","amountMinor":0,"currency":"BRL","occurredOn":"2026-10-03"}""")
            }
            assertEquals(400, invalidAmount.status.value)
        } finally {
            cleanupUser(email)
        }
    }

    @Test
    fun transferApiCancelKeepsBalancesUnchanged() = testApplication {
        assumeTrue(hasPostgres())
        val email = uniqueEmail("transfer-cancel")
        application { module() }
        try {
            val token = registerAndLogin(client, email, "password-123", "Cancel User")
            val source = createAccount(client, token, "Source", 20_000)
            val destination = createAccount(client, token, "Destination", 20_000)
            val create = client.post("/api/v1/transfers") {
                bearer(token)
                jsonBody("""{"sourceAccountId":"$source","destinationAccountId":"$destination","amountMinor":5000,"currency":"BRL","occurredOn":"2026-10-03"}""")
            }
            assertEquals(201, create.status.value, create.bodyAsText())
            val id = json(create.bodyAsText())["id"]!!.jsonPrimitive.content
            val cancelled = client.post("/api/v1/transfers/$id/cancel") { bearer(token) }
            assertEquals(200, cancelled.status.value, cancelled.bodyAsText())
            assertTrue(cancelled.bodyAsText().contains(""""status":"CANCELLED""""), cancelled.bodyAsText())

            val sourceBalance = client.get("/api/v1/accounts/$source") { bearer(token) }
            val destinationBalance = client.get("/api/v1/accounts/$destination") { bearer(token) }
            assertEquals(20000L, json(sourceBalance.bodyAsText())["balanceMinor"]!!.jsonPrimitive.content.toLong())
            assertEquals(20000L, json(destinationBalance.bodyAsText())["balanceMinor"]!!.jsonPrimitive.content.toLong())
        } finally {
            cleanupUser(email)
        }
    }

    private suspend fun registerAndLogin(client: HttpClient, email: String, password: String, displayName: String): String {
        val register = client.post("/api/v1/auth/register") {
            jsonBody("""{"email":"$email","displayName":"$displayName","password":"$password","timeZone":"UTC"}""")
        }
        assertEquals(201, register.status.value, register.bodyAsText())
        val login = client.post("/api/v1/auth/login") {
            jsonBody("""{"email":"$email","password":"$password"}""")
        }
        assertEquals(200, login.status.value, login.bodyAsText())
        return json(login.bodyAsText())["token"]!!.jsonPrimitive.content
    }

    private suspend fun createAccount(client: HttpClient, token: String, name: String, openingBalance: Long): String {
        val response = client.post("/api/v1/accounts") {
            bearer(token)
            jsonBody("""{"name":"$name","type":"CHECKING","openingBalanceMinor":$openingBalance,"currency":"BRL"}""")
        }
        assertEquals(201, response.status.value, response.bodyAsText())
        return json(response.bodyAsText())["id"]!!.jsonPrimitive.content
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

    private fun cleanupUser(email: String) {
        DatabaseConnectionProvider().open().use { c ->
            val userId = c.prepareStatement("SELECT id FROM users WHERE email = ?").use { s ->
                s.setString(1, email)
                s.executeQuery().use { r -> if (r.next()) r.getObject("id", UUID::class.java) else null }
            } ?: return
            c.autoCommit = false
            try {
                c.prepareStatement("DELETE FROM transfers WHERE user_id = ?").use { s ->
                    s.setObject(1, userId)
                    s.executeUpdate()
                }
                c.prepareStatement("UPDATE transactions SET related_transaction_id = NULL WHERE account_id IN (SELECT id FROM accounts WHERE owner_id = ?)").use { s ->
                    s.setObject(1, userId)
                    s.executeUpdate()
                }
                c.prepareStatement("DELETE FROM transactions WHERE account_id IN (SELECT id FROM accounts WHERE owner_id = ?)").use { s ->
                    s.setObject(1, userId)
                    s.executeUpdate()
                }
                c.prepareStatement("DELETE FROM accounts WHERE owner_id = ?").use { s ->
                    s.setObject(1, userId)
                    s.executeUpdate()
                }
                c.prepareStatement("DELETE FROM users WHERE id = ?").use { s ->
                    s.setObject(1, userId)
                    s.executeUpdate()
                }
                c.commit()
            } catch (e: Exception) {
                runCatching { c.rollback() }
                throw e
            } finally {
                c.autoCommit = true
            }
        }
    }
}
