package com.nexa.finance

import com.nexa.finance.infrastructure.database.DatabaseConnectionProvider
import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.get
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.server.testing.testApplication
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.jupiter.api.Assumptions.assumeTrue

class SyncConcurrencyApiTest {
    @Test
    fun concurrentIdenticalUpdatesConvergeSafelyAgainstPostgres() = testApplication {
        assumeTrue(hasPostgres())
        val email = uniqueEmail("sync-concurrency")
        application { module() }

        try {
            val token = registerAndLogin(client, email, "sync-concurrency-password", "Sync Concurrency User")

            val account = client.post("/api/v1/accounts") {
                bearer(token)
                jsonBody(
                    """{"name":"Sync Concurrency Account","type":"CHECKING","openingBalanceMinor":100000,"currency":"BRL"}"""
                )
            }
            assertEquals(201, account.status.value, account.bodyAsText())
            val accountId = json(account.bodyAsText())["id"]!!.jsonPrimitive.content

            val entityId = UUID.randomUUID().toString()
            val initialOperationId = UUID.randomUUID().toString()
            val initialPayload =
                """{"id":"$entityId","accountId":"$accountId","amountMinor":2500,"currency":"BRL","type":"EXPENSE","direction":"DEBIT","occurredOn":"2026-10-04","description":"Concurrent sync baseline","source":"MANUAL"}"""
            val initialPush = client.post("/api/v1/sync/push") {
                bearer(token)
                jsonBody(
                    """{"operations":[{"operationId":"$initialOperationId","entityType":"TRANSACTION","entityId":"$entityId","operation":"UPSERT","payload":$initialPayload}]}"""
                )
            }
            assertEquals(200, initialPush.status.value, initialPush.bodyAsText())
            assertEquals("ACKNOWLEDGED", acknowledgementStatus(initialPush.bodyAsText()))
            assertEquals(1L, acknowledgementVersion(initialPush.bodyAsText()))

            val updatePayload =
                """{"id":"$entityId","accountId":"$accountId","amountMinor":2600,"currency":"BRL","type":"EXPENSE","direction":"DEBIT","occurredOn":"2026-10-04","description":"Concurrent sync update","source":"MANUAL","baseVersion":1}"""
            val operationBodies = (1..16).map {
                val operationId = UUID.randomUUID().toString()
                """{"operations":[{"operationId":"$operationId","entityType":"TRANSACTION","entityId":"$entityId","operation":"UPSERT","payload":$updatePayload}]}"""
            }

            val results = coroutineScope {
                operationBodies.map { body ->
                    async(Dispatchers.IO) {
                        client.post("/api/v1/sync/push") {
                            bearer(token)
                            jsonBody(body)
                        }.let { response ->
                            response.status.value to response.bodyAsText()
                        }
                    }
                }.awaitAll()
            }

            assertTrue(
                results.all { it.first == 200 },
                "Concurrent sync requests returned unexpected statuses: ${results.map { it.first }}"
            )
            val statuses = results.map { acknowledgementStatus(it.second) }
            assertTrue(
                statuses.all { it == "ACKNOWLEDGED" || it == "CONFLICT" },
                "Unexpected concurrent sync acknowledgement statuses: $statuses"
            )
            assertTrue(
                statuses.any { it == "ACKNOWLEDGED" },
                "No concurrent sync request was acknowledged: $results"
            )

            val transaction = client.get("/api/v1/transactions/$entityId") {
                bearer(token)
            }
            assertEquals(200, transaction.status.value, transaction.bodyAsText())
            assertEquals(2600L, json(transaction.bodyAsText())["amountMinor"]!!.jsonPrimitive.content.toLong())
            assertEquals(2L, json(transaction.bodyAsText())["version"]!!.jsonPrimitive.content.toLong())

            val pull = client.get("/api/v1/sync/pull?cursor=0&limit=100") {
                bearer(token)
            }
            assertEquals(200, pull.status.value, pull.bodyAsText())
            assertTrue(pull.bodyAsText().contains(entityId))
        } finally {
            cleanupUsers(email)
        }
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
        assertEquals(201, register.status.value, register.bodyAsText())

        val login = client.post("/api/v1/auth/login") {
            jsonBody("""{"email":"$email","password":"$password"}""")
        }
        assertEquals(200, login.status.value, login.bodyAsText())
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
    private fun acknowledgementStatus(body: String): String =
        json(body)["acknowledgements"]!!.jsonArray.single()["status"]!!.jsonPrimitive.content

    private fun acknowledgementVersion(body: String): Long =
        json(body)["acknowledgements"]!!.jsonArray.single()["version"]!!.jsonPrimitive.content.toLong()

    private fun uniqueEmail(prefix: String): String = prefix + "-" + UUID.randomUUID() + "@example.com"
    private fun hasPostgres(): Boolean = !System.getenv("POSTGRES_PASSWORD").isNullOrBlank()

    private fun cleanupUsers(vararg emails: String) {
        DatabaseConnectionProvider().open().use { connection ->
            connection.autoCommit = false
            try {
                connection.prepareStatement(
                    "DELETE FROM transactions WHERE account_id IN (SELECT a.id FROM accounts a JOIN users u ON u.id = a.owner_id WHERE u.email = ANY (?))"
                ).use { statement ->
                    statement.setArray(1, connection.createArrayOf("text", emails))
                    statement.executeUpdate()
                }
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
