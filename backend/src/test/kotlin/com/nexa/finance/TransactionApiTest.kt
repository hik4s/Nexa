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
            assertEquals(1, json(create.bodyAsText())["version"]!!.jsonPrimitive.content.toLong())
            val update = client.put("/api/v1/transactions/$transactionId") {
                bearer(ownerToken)
                jsonBody("""{"baseVersion":1,"accountId":"$accountId","amountMinor":13000,"currency":"BRL","type":"EXPENSE","direction":"DEBIT","occurredOn":"2026-10-03","description":"Mercado atualizado","source":"MANUAL"}""")
            }
            assertEquals(200, update.status.value, update.bodyAsText())
            assertEquals(2, json(update.bodyAsText())["version"]!!.jsonPrimitive.content.toLong())
            assertTrue(update.bodyAsText().contains("Mercado atualizado"))

            val stale = client.put("/api/v1/transactions/$transactionId") {
                bearer(ownerToken)
                jsonBody("""{"baseVersion":1,"accountId":"$accountId","amountMinor":14000,"currency":"BRL","type":"EXPENSE","direction":"DEBIT","occurredOn":"2026-10-03","description":"Stale edit","source":"MANUAL"}""")
            }
            assertEquals(409, stale.status.value, stale.bodyAsText())
            assertTrue(stale.bodyAsText().contains("CONFLICT"), stale.bodyAsText())

            val secondCreate = client.post("/api/v1/transactions") {
                bearer(ownerToken)
                jsonBody("""{"accountId":"$accountId","amountMinor":5000,"currency":"BRL","type":"EXPENSE","direction":"DEBIT","occurredOn":"2026-10-04","description":"Padaria","source":"MANUAL"}""")
            }
            assertEquals(201, secondCreate.status.value, secondCreate.bodyAsText())
            val secondTransactionId = json(secondCreate.bodyAsText())["id"]!!.jsonPrimitive.content

            val ownerList = client.get("/api/v1/transactions?accountId=$accountId") { bearer(ownerToken) }
            assertEquals(200, ownerList.status.value)
            assertTrue(ownerList.bodyAsText().contains(transactionId))
            assertTrue(ownerList.bodyAsText().contains(secondTransactionId))

            val paged = client.get("/api/v1/transactions?accountId=$accountId&limit=1&offset=1") { bearer(ownerToken) }
            assertEquals(200, paged.status.value, paged.bodyAsText())
            assertTrue(paged.bodyAsText().contains(transactionId), paged.bodyAsText())
            assertFalse(paged.bodyAsText().contains(secondTransactionId), paged.bodyAsText())
            val otherList = client.get("/api/v1/transactions?accountId=$accountId") { bearer(otherToken) }
            assertEquals(200, otherList.status.value)
            assertFalse(otherList.bodyAsText().contains(transactionId))
            assertEquals(400, client.get("/api/v1/transactions/$transactionId") { bearer(otherToken) }.status.value)
            assertEquals(400, client.post("/api/v1/transactions/$transactionId/cancel") { bearer(otherToken) }.status.value)
            val balance = client.get("/api/v1/accounts/$accountId") { bearer(ownerToken) }
            assertEquals(200, balance.status.value, balance.bodyAsText())
            assertTrue(balance.bodyAsText().contains(""""balanceMinor":32500"""), balance.bodyAsText())
        } finally {
            cleanupUsers(ownerEmail, otherEmail)
        }
    }

    @Test
    fun transactionApiIsIdempotentForClientGeneratedTransactionId() = testApplication {
        assumeTrue(hasPostgres())
        val email = uniqueEmail("tx-idempotent")
        application { module() }
        try {
            val token = registerAndLogin(client, email, "password-123", "Idempotent User")
            val account = client.post("/api/v1/accounts") {
                bearer(token)
                jsonBody("""{"name":"Idempotent Account","type":"CHECKING","openingBalanceMinor":10000,"currency":"BRL"}""")
            }
            assertEquals(201, account.status.value)
            val accountId = json(account.bodyAsText())["id"]!!.jsonPrimitive.content
            val transactionId = UUID.randomUUID().toString()
            val body = """{"id":"$transactionId","accountId":"$accountId","amountMinor":2500,"currency":"BRL","type":"EXPENSE","direction":"DEBIT","occurredOn":"2026-10-04","description":"Offline retry","source":"MANUAL"}"""

            val first = client.post("/api/v1/transactions") {
                bearer(token)
                jsonBody(body)
            }
            val second = client.post("/api/v1/transactions") {
                bearer(token)
                jsonBody(body)
            }

            assertEquals(201, first.status.value, first.bodyAsText())
            assertEquals(201, second.status.value, second.bodyAsText())
            assertEquals(transactionId, json(first.bodyAsText())["id"]!!.jsonPrimitive.content)
            assertEquals(transactionId, json(second.bodyAsText())["id"]!!.jsonPrimitive.content)

            val list = client.get("/api/v1/transactions?accountId=$accountId") { bearer(token) }
            assertEquals(200, list.status.value)
            assertEquals(1, list.bodyAsText().split(transactionId).size - 1)
        } finally {
            cleanupUsers(email)
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

    @Test
    fun syncPushPullIsIdempotentAndCursorBasedAgainstPostgres() = testApplication {
        assumeTrue(hasPostgres())
        val email = uniqueEmail("sync")
        application { module() }
        try {
            val token = registerAndLogin(client, email, "sync-password", "Sync User")
            val account = client.post("/api/v1/accounts") {
                bearer(token)
                jsonBody("""{"name":"Sync Account","type":"CHECKING","openingBalanceMinor":100000,"currency":"BRL"}""")
            }
            assertEquals(201, account.status.value, account.bodyAsText())
            val accountId = json(account.bodyAsText())["id"]!!.jsonPrimitive.content
            val entityId = UUID.randomUUID().toString()
            val operationId = UUID.randomUUID().toString()
            val payload = """{"id":"$entityId","accountId":"$accountId","amountMinor":2500,"currency":"BRL","type":"EXPENSE","direction":"DEBIT","occurredOn":"2026-10-04","description":"Offline sync","source":"MANUAL"}"""
            val pushBody = """{"operations":[{"operationId":"$operationId","entityType":"TRANSACTION","entityId":"$entityId","operation":"UPSERT","payload":$payload}]}"""

            val firstPush = client.post("/api/v1/sync/push") { bearer(token); jsonBody(pushBody) }
            assertEquals(200, firstPush.status.value, firstPush.bodyAsText())
            assertTrue(firstPush.bodyAsText().contains("ACKNOWLEDGED"))
            assertTrue(firstPush.bodyAsText().contains("\"version\":1"), firstPush.bodyAsText())
            val firstCursor = json(firstPush.bodyAsText())["cursor"]!!.jsonPrimitive.content.toLong()

            val updatePayload = payload.replace("\"source\":\"MANUAL\"}", "\"source\":\"MANUAL\",\"baseVersion\":1}")
                .replace("\"amountMinor\":2500", "\"amountMinor\":2600")
            val updateBody = """{"operations":[{"operationId":"${UUID.randomUUID()}","entityType":"TRANSACTION","entityId":"$entityId","operation":"UPSERT","payload":$updatePayload}]}"""
            val updatePush = client.post("/api/v1/sync/push") { bearer(token); jsonBody(updateBody) }
            assertEquals(200, updatePush.status.value, updatePush.bodyAsText())
            assertTrue(updatePush.bodyAsText().contains("\"status\":\"ACKNOWLEDGED\""), updatePush.bodyAsText())
            assertTrue(updatePush.bodyAsText().contains("\"version\":2"), updatePush.bodyAsText())

            val stalePayload = updatePayload.replace("\"amountMinor\":2600", "\"amountMinor\":2700")
            val staleBody = """{"operations":[{"operationId":"${UUID.randomUUID()}","entityType":"TRANSACTION","entityId":"$entityId","operation":"UPSERT","payload":$stalePayload}]}"""
            val stalePush = client.post("/api/v1/sync/push") { bearer(token); jsonBody(staleBody) }
            assertEquals(200, stalePush.status.value, stalePush.bodyAsText())
            assertTrue(stalePush.bodyAsText().contains("\"status\":\"CONFLICT\""), stalePush.bodyAsText())

            val retryUpdate = client.post("/api/v1/sync/push") { bearer(token); jsonBody(updateBody) }
            assertEquals(200, retryUpdate.status.value, retryUpdate.bodyAsText())
            assertTrue(retryUpdate.bodyAsText().contains("\"status\":\"ACKNOWLEDGED\""), retryUpdate.bodyAsText())
            assertTrue(retryUpdate.bodyAsText().contains("\"version\":2"), retryUpdate.bodyAsText())

            val conflictBody = """{"operations":[{"operationId":"${UUID.randomUUID()}","entityType":"TRANSACTION","entityId":"$entityId","operation":"UPSERT","payload":$stalePayload}]}"""
            val conflictPush = client.post("/api/v1/sync/push") { bearer(token); jsonBody(conflictBody) }
            assertEquals(200, conflictPush.status.value, conflictPush.bodyAsText())
            assertTrue(conflictPush.bodyAsText().contains("\"status\":\"CONFLICT\""), conflictPush.bodyAsText())

            val pull = client.get("/api/v1/sync/pull?cursor=0&limit=50") { bearer(token) }
            assertEquals(200, pull.status.value, pull.bodyAsText())
            assertTrue(pull.bodyAsText().contains(entityId))
            assertTrue(pull.bodyAsText().contains("Offline sync"))
            val cursor = json(pull.bodyAsText())["cursor"]!!.jsonPrimitive.content.toLong()

            val empty = client.get("/api/v1/sync/pull?cursor=$cursor&limit=50") { bearer(token) }
            assertEquals(200, empty.status.value)
            assertEquals("[]", json(empty.bodyAsText())["changes"]!!.toString())
        } finally {
            cleanupUsers(email)
        }
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