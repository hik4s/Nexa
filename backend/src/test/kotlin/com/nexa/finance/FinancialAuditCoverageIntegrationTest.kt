package com.nexa.finance

import com.nexa.finance.infrastructure.database.DatabaseConnectionProvider
import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.jupiter.api.Assumptions.assumeTrue

class FinancialAuditCoverageIntegrationTest {
    @Test
    fun accountAndTransactionMutationsProduceAuditEventsWithoutSensitivePayload() = testApplication {
        assumeTrue(!System.getenv("POSTGRES_PASSWORD").isNullOrBlank())
        val email = "audit-" + UUID.randomUUID() + "@example.com"
        application { module() }

        try {
            val token = registerAndLogin(client, email, "audit-password", "Audit User")

            val account = client.post("/api/v1/accounts") {
                bearer(token)
                jsonBody("""{"name":"Audit Checking","type":"CHECKING","openingBalanceMinor":100000,"currency":"BRL"}""")
            }
            assertEquals(201, account.status.value, account.bodyAsText())
            val accountId = json(account.bodyAsText())["id"]!!.jsonPrimitive.content

            val transaction = client.post("/api/v1/transactions") {
                bearer(token)
                jsonBody("""{"accountId":"$accountId","amountMinor":2500,"currency":"BRL","type":"EXPENSE","direction":"DEBIT","occurredOn":"2026-10-06","description":"Audit transaction","source":"MANUAL"}""")
            }
            assertEquals(201, transaction.status.value, transaction.bodyAsText())
            val transactionId = json(transaction.bodyAsText())["id"]!!.jsonPrimitive.content

            val update = client.put("/api/v1/transactions/$transactionId") {
                bearer(token)
                jsonBody("""{"baseVersion":1,"accountId":"$accountId","amountMinor":3000,"currency":"BRL","type":"EXPENSE","direction":"DEBIT","occurredOn":"2026-10-06","description":"Audit transaction updated","source":"MANUAL"}""")
            }
            assertEquals(200, update.status.value, update.bodyAsText())

            val post = client.post("/api/v1/transactions/$transactionId/post") {
                bearer(token)
            }
            assertEquals(200, post.status.value, post.bodyAsText())

            val cancel = client.post("/api/v1/transactions/$transactionId/cancel") {
                bearer(token)
            }
            assertEquals(200, cancel.status.value, cancel.bodyAsText())

            val events = auditEventsFor(email)

            assertTrue(events.contains("ACCOUNT_CREATED"))
            assertTrue(events.count { it == "TRANSACTION_CREATED" } >= 1)
            assertTrue(events.contains("TRANSACTION_UPDATED"))
            assertTrue(events.contains("TRANSACTION_POSTED"))
            assertTrue(events.contains("TRANSACTION_CANCELLED"))
            assertTrue(events.none { it.contains("password", ignoreCase = true) })
            assertTrue(events.none { it.contains("token", ignoreCase = true) })
        } finally {
            cleanup(email)
        }
    }

    private fun auditEventsFor(email: String): List<String> =
        DatabaseConnectionProvider().open().use { connection ->
            connection.prepareStatement(
                """
                SELECT ae.event_type, ae.metadata
                FROM audit_events ae
                LEFT JOIN users u ON u.id = ae.actor_user_id
                WHERE u.email = ?
                ORDER BY ae.created_at ASC, ae.id ASC
                """.trimIndent(),
            ).use { statement ->
                statement.setString(1, email)
                statement.executeQuery().use { result ->
                    buildList {
                        while (result.next()) {
                            add(result.getString("event_type"))
                            assertTrue(!(result.getString("metadata") ?: "").contains("password", ignoreCase = true))
                            assertTrue(!(result.getString("metadata") ?: "").contains("token", ignoreCase = true))
                        }
                    }
                }
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

    private fun cleanup(email: String) {
        DatabaseConnectionProvider().open().use { connection ->
            connection.autoCommit = false
            try {
                connection.prepareStatement("DELETE FROM audit_events WHERE actor_user_id IN (SELECT id FROM users WHERE email = ?)").use { statement ->
                    statement.setString(1, email)
                    statement.executeUpdate()
                }
                connection.prepareStatement("DELETE FROM transactions WHERE account_id IN (SELECT id FROM accounts WHERE owner_id IN (SELECT id FROM users WHERE email = ?))").use { statement ->
                    statement.setString(1, email)
                    statement.executeUpdate()
                }
                connection.prepareStatement("DELETE FROM accounts WHERE owner_id IN (SELECT id FROM users WHERE email = ?)").use { statement ->
                    statement.setString(1, email)
                    statement.executeUpdate()
                }
                connection.prepareStatement("DELETE FROM users WHERE email = ?").use { statement ->
                    statement.setString(1, email)
                    statement.executeUpdate()
                }
                connection.commit()
            } catch (e: Exception) {
                connection.rollback()
                throw e
            }
        }
    }

    private fun io.ktor.client.request.HttpRequestBuilder.bearer(token: String) {
        header(HttpHeaders.Authorization, "Bearer " + token)
    }

    private fun io.ktor.client.request.HttpRequestBuilder.jsonBody(body: String) {
        header(HttpHeaders.ContentType, ContentType.Application.Json)
        setBody(body)
    }

    private fun json(body: String) = Json.parseToJsonElement(body).jsonObject
}