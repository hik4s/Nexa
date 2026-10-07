package com.nexa.finance

import com.nexa.finance.infrastructure.database.DatabaseConnectionProvider
import io.ktor.client.HttpClient
import io.ktor.client.request.*
import io.ktor.client.statement.bodyAsText
import io.ktor.http.*
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import java.util.UUID
import kotlin.test.*

@Execution(ExecutionMode.SAME_THREAD)
class RecurrenceApiTest {
    @Test
    fun recurrenceLifecycleOccurrencesAndIdempotentExecutionWorkAgainstPostgres() = testApplication {
        assumeTrue(hasPostgres())
        val email = uniqueEmail("recurrence")
        application { module() }
        try {
            val token = registerAndLogin(client, email, "password-123", "Recurring User")
            val account = createAccount(client, token, "Checking", 100_000)
            val created = client.post("/api/v1/recurrences") { bearer(token); jsonBody("""{"accountId":"$account","amountMinor":10000,"currency":"BRL","transactionType":"EXPENSE","frequency":"MONTHLY","interval":1,"startOn":"2026-10-03","maxOccurrences":3,"description":"Rent"}""") }
            assertEquals(201, created.status.value, created.bodyAsText())
            val id = json(created.bodyAsText())["id"]!!.jsonPrimitive.content
            val occurrences = client.get("/api/v1/recurrences/$id/occurrences?from=2026-10-01&to=2027-01-31") { bearer(token) }
            assertEquals(200, occurrences.status.value, occurrences.bodyAsText())
            assertEquals(3, Json.parseToJsonElement(occurrences.bodyAsText()).jsonObject["occurrences"]!!.toString().count { it == '{' })
            val execution = client.post("/api/v1/recurrences/$id/occurrences/1/execute") { bearer(token) }
            assertEquals(201, execution.status.value, execution.bodyAsText())
            val firstTxId = json(execution.bodyAsText())["id"]!!.jsonPrimitive.content
            val duplicate = client.post("/api/v1/recurrences/$id/occurrences/1/execute") { bearer(token) }
            assertEquals(201, duplicate.status.value, duplicate.bodyAsText())
            assertEquals(firstTxId, json(duplicate.bodyAsText())["id"]!!.jsonPrimitive.content)
            val balance = client.get("/api/v1/accounts/$account") { bearer(token) }
            assertEquals(90_000L, json(balance.bodyAsText())["balanceMinor"]!!.jsonPrimitive.content.toLong())
            val second = client.post("/api/v1/recurrences/$id/occurrences/2/execute") { bearer(token) }
            assertEquals(201, second.status.value, second.bodyAsText())
            assertEquals("2026-11-03", json(second.bodyAsText())["occurredOn"]!!.jsonPrimitive.content)
        } finally { cleanupUser(email) }
    }

    @Test
    fun pausedRecurrenceCannotExecuteAndResumeRestoresExecution() = testApplication {
        assumeTrue(hasPostgres())
        val email = uniqueEmail("recurrence-pause")
        application { module() }
        try {
            val token = registerAndLogin(client, email, "password-123", "Pause User")
            val account = createAccount(client, token, "Checking", 100_000)
            val created = client.post("/api/v1/recurrences") { bearer(token); jsonBody("""{"accountId":"$account","amountMinor":5000,"currency":"BRL","transactionType":"INCOME","frequency":"WEEKLY","startOn":"2026-10-03"}""") }
            val id = json(created.bodyAsText())["id"]!!.jsonPrimitive.content
            assertEquals(200, client.post("/api/v1/recurrences/$id/pause") { bearer(token) }.status.value)
            assertEquals(400, client.post("/api/v1/recurrences/$id/occurrences/1/execute") { bearer(token) }.status.value)
            assertEquals(200, client.post("/api/v1/recurrences/$id/resume") { bearer(token) }.status.value)
            assertEquals(201, client.post("/api/v1/recurrences/$id/occurrences/1/execute") { bearer(token) }.status.value)
            assertEquals(200, client.delete("/api/v1/recurrences/$id") { bearer(token) }.status.value)
            assertEquals(400, client.post("/api/v1/recurrences/$id/occurrences/2/execute") { bearer(token) }.status.value)
        } finally { cleanupUser(email) }
    }

    @Test
    fun recurrenceIsolatedByOwnerAndValidatesAccountCurrency() = testApplication {
        assumeTrue(hasPostgres())
        val ownerEmail = uniqueEmail("recurrence-owner")
        val otherEmail = uniqueEmail("recurrence-other")
        application { module() }
        try {
            val owner = registerAndLogin(client, ownerEmail, "password-123", "Owner")
            val other = registerAndLogin(client, otherEmail, "password-123", "Other")
            val account = createAccount(client, owner, "Checking", 100_000)
            val created = client.post("/api/v1/recurrences") { bearer(owner); jsonBody("""{"accountId":"$account","amountMinor":10000,"currency":"BRL","transactionType":"EXPENSE","frequency":"DAILY","startOn":"2026-10-03"}""") }
            assertEquals(201, created.status.value)
            val id = json(created.bodyAsText())["id"]!!.jsonPrimitive.content
            assertEquals(400, client.get("/api/v1/recurrences/$id") { bearer(other) }.status.value)
            val wrongCurrency = client.post("/api/v1/recurrences") { bearer(owner); jsonBody("""{"accountId":"$account","amountMinor":10000,"currency":"USD","transactionType":"EXPENSE","frequency":"DAILY","startOn":"2026-10-03"}""") }
            assertEquals(400, wrongCurrency.status.value)
        } finally { cleanupUser(ownerEmail); cleanupUser(otherEmail) }
    }

    @Test
    fun invalidRecurrenceBoundsAreRejectedByDomainAndApi() = testApplication {
        assumeTrue(hasPostgres())
        val email = uniqueEmail("recurrence-invalid")
        application { module() }
        try {
            val token = registerAndLogin(client, email, "password-123", "Invalid User")
            val account = createAccount(client, token, "Checking", 0)
            val response = client.post("/api/v1/recurrences") { bearer(token); jsonBody("""{"accountId":"$account","amountMinor":10000,"currency":"BRL","transactionType":"EXPENSE","frequency":"DAILY","interval":0,"startOn":"2026-10-03"}""") }
            assertEquals(400, response.status.value, response.bodyAsText())
            val invalidCategory = client.post("/api/v1/recurrences") {
                bearer(token)
                jsonBody("""{"accountId":"$account","amountMinor":10000,"currency":"BRL","transactionType":"EXPENSE","frequency":"DAILY","startOn":"2026-10-03","categoryId":"00000000-0000-0000-0000-000000000001"}""")
            }
            assertEquals(400, invalidCategory.status.value, invalidCategory.bodyAsText())
        } finally { cleanupUser(email) }
    }

    @Test
    fun recurrenceCanBeEditedAndReturnsNextOccurrenceSummary() = testApplication {
        assumeTrue(hasPostgres())
        val email = uniqueEmail("recurrence-edit")
        application { module() }
        try {
            val token = registerAndLogin(client, email, "password-123", "Edit User")
            val account = createAccount(client, token, "Checking", 100_000)
            val created = client.post("/api/v1/recurrences") {
                bearer(token)
                jsonBody("""{"accountId":"$account","amountMinor":9000,"currency":"BRL","transactionType":"EXPENSE","frequency":"MONTHLY","startOn":"2026-11-04","description":"Internet"}""")
            }
            assertEquals(201, created.status.value, created.bodyAsText())
            val id = json(created.bodyAsText())["id"]!!.jsonPrimitive.content
            assertTrue(created.bodyAsText().contains("\"nextOccurrenceOn\""))

            val updated = client.put("/api/v1/recurrences/$id") {
                bearer(token)
                jsonBody("""{"accountId":"$account","amountMinor":12000,"currency":"BRL","transactionType":"EXPENSE","frequency":"WEEKLY","interval":1,"startOn":"2026-11-04","description":"Internet fibra"}""")
            }
            assertEquals(200, updated.status.value, updated.bodyAsText())
            assertTrue(updated.bodyAsText().contains("\"amountMinor\":12000"))
            assertTrue(updated.bodyAsText().contains("\"frequency\":\"WEEKLY\""))
            assertTrue(updated.bodyAsText().contains("Internet fibra"))
            assertTrue(updated.bodyAsText().contains("\"nextOccurrences\""))
        } finally {
            cleanupUser(email)
        }
    }

    @Test
    fun detectsPossibleRecurringTransactions() = testApplication {
        assumeTrue(hasPostgres())
        val email = uniqueEmail("recurrence-detect")
        application { module() }
        try {
            val token = registerAndLogin(client, email, "password-123", "Detector User")
            val account = createAccount(client, token, "Checking", 100_000)
            listOf("2026-09-13", "2026-09-20", "2026-09-27").forEach { date ->
                val tx = client.post("/api/v1/transactions") {
                    bearer(token)
                    jsonBody("""{"accountId":"$account","amountMinor":15990,"currency":"BRL","type":"EXPENSE","direction":"DEBIT","occurredOn":"$date","description":"Streaming","source":"MANUAL"}""")
                }
                assertEquals(201, tx.status.value, tx.bodyAsText())
                assertEquals(
                    "POSTED",
                    json(tx.bodyAsText())["status"]!!.jsonPrimitive.content,
                )
            }

            val candidates = client.get("/api/v1/recurrences/candidates") { bearer(token) }
            assertEquals(200, candidates.status.value, candidates.bodyAsText())
            val candidateBody = candidates.bodyAsText()
            assertTrue(candidateBody.contains("Streaming"), candidateBody)
            assertTrue(candidateBody.contains("\"frequency\":\"WEEKLY\""), candidateBody)
            assertTrue(candidateBody.contains("\"confidencePercent\":"), candidateBody)
        } finally {
            cleanupUser(email)
        }
    }

    private suspend fun registerAndLogin(client: HttpClient, email: String, password: String, name: String): String {
        assertEquals(201, client.post("/api/v1/auth/register") { jsonBody("""{"email":"$email","displayName":"$name","password":"$password","timeZone":"UTC"}""") }.status.value)
        val login = client.post("/api/v1/auth/login") { jsonBody("""{"email":"$email","password":"$password"}""") }
        assertEquals(200, login.status.value, login.bodyAsText())
        return json(login.bodyAsText())["token"]!!.jsonPrimitive.content
    }
    private suspend fun createAccount(client: HttpClient, token: String, name: String, openingBalance: Long): String {
        val response = client.post("/api/v1/accounts") { bearer(token); jsonBody("""{"name":"$name","type":"CHECKING","openingBalanceMinor":$openingBalance,"currency":"BRL"}""") }
        assertEquals(201, response.status.value, response.bodyAsText())
        return json(response.bodyAsText())["id"]!!.jsonPrimitive.content
    }
    private fun io.ktor.client.request.HttpRequestBuilder.jsonBody(body: String) { header(HttpHeaders.ContentType, ContentType.Application.Json.toString()); setBody(body) }
    private fun io.ktor.client.request.HttpRequestBuilder.bearer(token: String) { header(HttpHeaders.Authorization, "Bearer $token") }
    private fun json(body: String) = Json.parseToJsonElement(body).jsonObject
    private fun uniqueEmail(prefix: String) = prefix + "-" + UUID.randomUUID() + "@example.com"
    private fun hasPostgres() = !System.getenv("POSTGRES_PASSWORD").isNullOrBlank()

    private fun cleanupUser(email: String) {
        DatabaseConnectionProvider().open().use { c ->
            val userId = c.prepareStatement("SELECT id FROM users WHERE email = ?").use { s ->
                s.setString(1, email); s.executeQuery().use { r -> if (r.next()) r.getObject("id", UUID::class.java) else null }
            } ?: return
            c.autoCommit = false
            try {
                c.prepareStatement("DELETE FROM invoice_payment_transactions WHERE invoice_id IN (SELECT id FROM invoices WHERE card_id IN (SELECT id FROM cards WHERE owner_id = ?))").use { s -> s.setObject(1, userId); s.executeUpdate() }
                c.prepareStatement("UPDATE transactions SET related_transaction_id = NULL WHERE account_id IN (SELECT id FROM accounts WHERE owner_id = ?)").use { s -> s.setObject(1, userId); s.executeUpdate() }
                c.prepareStatement("DELETE FROM transactions WHERE account_id IN (SELECT id FROM accounts WHERE owner_id = ?)").use { s -> s.setObject(1, userId); s.executeUpdate() }
                c.prepareStatement("DELETE FROM recurrences WHERE user_id = ?").use { s -> s.setObject(1, userId); s.executeUpdate() }
                c.prepareStatement("DELETE FROM invoices WHERE card_id IN (SELECT id FROM cards WHERE owner_id = ?)").use { s -> s.setObject(1, userId); s.executeUpdate() }
                c.prepareStatement("DELETE FROM cards WHERE owner_id = ?").use { s -> s.setObject(1, userId); s.executeUpdate() }
                c.prepareStatement("DELETE FROM accounts WHERE owner_id = ?").use { s -> s.setObject(1, userId); s.executeUpdate() }
                c.prepareStatement("DELETE FROM users WHERE id = ?").use { s -> s.setObject(1, userId); s.executeUpdate() }
                c.commit()
            } catch (e: Exception) { c.rollback(); throw e } finally { c.autoCommit = true }
        }
    }
}
