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
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Assumptions.assumeTrue
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RefundCancellationApiTest {
    @Test
    fun expenseRefundIsPartialCumulativeAndCannotExceedOriginal() = testApplication {
        assumeTrue(hasPostgres())
        val email = uniqueEmail("refund-expense")
        application { module() }
        try {
            val token = registerAndLogin(client, email, "password-123", "Refund User")
            val account = createAccount(client, token, "Checking", 100_000, "CHECKING")
            val expense = client.post("/api/v1/transactions") {
                bearer(token)
                jsonBody("""{"accountId":"$account","amountMinor":50000,"currency":"BRL","type":"EXPENSE","direction":"DEBIT","occurredOn":"2026-10-03","description":"Food"}""")
            }
            assertEquals(201, expense.status.value, expense.bodyAsText())
            val originalId = json(expense.bodyAsText())["id"]!!.jsonPrimitive.content

            val first = client.post("/api/v1/transactions/$originalId/refunds") {
                bearer(token)
                jsonBody("""{"amountMinor":20000,"currency":"BRL","occurredOn":"2026-10-04","description":"Partial refund"}""")
            }
            assertEquals(201, first.status.value, first.bodyAsText())
            assertTrue(first.bodyAsText().contains(""""type":"REFUND""""))
            assertTrue(first.bodyAsText().contains(""""direction":"CREDIT""""))

            val second = client.post("/api/v1/transactions/$originalId/refunds") {
                bearer(token)
                jsonBody("""{"amountMinor":30000,"currency":"BRL","occurredOn":"2026-10-05"}""")
            }
            assertEquals(201, second.status.value, second.bodyAsText())

            val overflow = client.post("/api/v1/transactions/$originalId/refunds") {
                bearer(token)
                jsonBody("""{"amountMinor":1,"currency":"BRL","occurredOn":"2026-10-06"}""")
            }
            assertEquals(400, overflow.status.value, overflow.bodyAsText())

            val balance = client.get("/api/v1/accounts/$account") { bearer(token) }
            assertEquals(100_000L, json(balance.bodyAsText())["balanceMinor"]!!.jsonPrimitive.content.toLong())
        } finally {
            cleanupUser(email)
        }
    }

    @Test
    fun cardPurchaseRefundReducesDebtAndRestoresLimitEvenWhenCardIsBlocked() = testApplication {
        assumeTrue(hasPostgres())
        val email = uniqueEmail("refund-card")
        application { module() }
        try {
            val token = registerAndLogin(client, email, "password-123", "Card Refund")
            val liability = createAccount(client, token, "Liability", 0, "CREDIT_CARD")
            val card = createCard(client, token, liability, 100_000)
            val cardId = json(card)["id"]!!.jsonPrimitive.content
            val purchase = client.post("/api/v1/cards/$cardId/purchases") {
                bearer(token)
                jsonBody("""{"amountMinor":60000,"currency":"BRL","occurredOn":"2026-10-03","post":true}""")
            }
            assertEquals(201, purchase.status.value, purchase.bodyAsText())
            val purchaseId = json(purchase.bodyAsText())["id"]!!.jsonPrimitive.content

            assertEquals(200, client.post("/api/v1/cards/$cardId/status") {
                bearer(token)
                jsonBody("""{"status":"BLOCKED"}""")
            }.status.value)

            val refund = client.post("/api/v1/transactions/$purchaseId/refunds") {
                bearer(token)
                jsonBody("""{"amountMinor":20000,"currency":"BRL","occurredOn":"2026-10-04"}""")
            }
            assertEquals(201, refund.status.value, refund.bodyAsText())
            assertTrue(refund.bodyAsText().contains(""""direction":"DEBIT""""))

            val cardAfter = client.get("/api/v1/cards/$cardId") { bearer(token) }
            assertEquals(60_000L, json(cardAfter.bodyAsText())["availableLimitMinor"]!!.jsonPrimitive.content.toLong())
        } finally {
            cleanupUser(email)
        }
    }

    @Test
    fun postedExpenseCancellationCreatesOppositeLedgerEventAndIsIdempotentlyBlocked() = testApplication {
        assumeTrue(hasPostgres())
        val email = uniqueEmail("cancellation")
        application { module() }
        try {
            val token = registerAndLogin(client, email, "password-123", "Cancellation User")
            val account = createAccount(client, token, "Checking", 100_000, "CHECKING")
            val expense = client.post("/api/v1/transactions") {
                bearer(token)
                jsonBody("""{"accountId":"$account","amountMinor":25000,"currency":"BRL","type":"EXPENSE","direction":"DEBIT","occurredOn":"2026-10-03","description":"Wrong charge"}""")
            }
            assertEquals(201, expense.status.value, expense.bodyAsText())
            val originalId = json(expense.bodyAsText())["id"]!!.jsonPrimitive.content

            val cancellation = client.post("/api/v1/transactions/$originalId/cancellation") {
                bearer(token)
                jsonBody("""{"occurredOn":"2026-10-04","description":"Correction"}""")
            }
            assertEquals(201, cancellation.status.value, cancellation.bodyAsText())
            assertTrue(cancellation.bodyAsText().contains(""""type":"CANCELLATION""""))
            assertTrue(cancellation.bodyAsText().contains(""""direction":"CREDIT""""))

            val duplicate = client.post("/api/v1/transactions/$originalId/cancellation") {
                bearer(token)
                jsonBody("""{"occurredOn":"2026-10-05"}""")
            }
            assertEquals(400, duplicate.status.value, duplicate.bodyAsText())

            val balance = client.get("/api/v1/accounts/$account") { bearer(token) }
            assertEquals(100_000L, json(balance.bodyAsText())["balanceMinor"]!!.jsonPrimitive.content.toLong())
        } finally {
            cleanupUser(email)
        }
    }

    @Test
    fun singleLegPaymentCanReceiveAccountingCancellation() = testApplication {
        assumeTrue(hasPostgres())
        val email = uniqueEmail("payment-cancellation")
        application { module() }
        try {
            val token = registerAndLogin(client, email, "password-123", "Payment Cancellation")
            val account = createAccount(client, token, "Checking", 100_000, "CHECKING")
            val payment = client.post("/api/v1/transactions") {
                bearer(token)
                jsonBody("""{"accountId":"$account","amountMinor":20000,"currency":"BRL","type":"PAYMENT","direction":"DEBIT","occurredOn":"2026-10-03"}""")
            }
            assertEquals(201, payment.status.value, payment.bodyAsText())
            val paymentId = json(payment.bodyAsText())["id"]!!.jsonPrimitive.content

            val cancellation = client.post("/api/v1/transactions/$paymentId/cancellation") {
                bearer(token)
                jsonBody("""{"occurredOn":"2026-10-04"}""")
            }
            assertEquals(201, cancellation.status.value, cancellation.bodyAsText())

            val balance = client.get("/api/v1/accounts/$account") { bearer(token) }
            assertEquals(100_000L, json(balance.bodyAsText())["balanceMinor"]!!.jsonPrimitive.content.toLong())
        } finally {
            cleanupUser(email)
        }
    }

    @Test
    fun refundAndCancellationRespectOwnerIsolation() = testApplication {
        assumeTrue(hasPostgres())
        val ownerEmail = uniqueEmail("refund-owner")
        val otherEmail = uniqueEmail("refund-other")
        application { module() }
        try {
            val owner = registerAndLogin(client, ownerEmail, "password-123", "Owner")
            val other = registerAndLogin(client, otherEmail, "password-123", "Other")
            val account = createAccount(client, owner, "Checking", 100_000, "CHECKING")
            val expense = client.post("/api/v1/transactions") {
                bearer(owner)
                jsonBody("""{"accountId":"$account","amountMinor":10000,"currency":"BRL","type":"EXPENSE","direction":"DEBIT","occurredOn":"2026-10-03"}""")
            }
            assertEquals(201, expense.status.value, expense.bodyAsText())
            val id = json(expense.bodyAsText())["id"]!!.jsonPrimitive.content

            val refund = client.post("/api/v1/transactions/$id/refunds") {
                bearer(other)
                jsonBody("""{"amountMinor":10000,"currency":"BRL","occurredOn":"2026-10-04"}""")
            }
            assertEquals(400, refund.status.value, refund.bodyAsText())

            val cancellation = client.post("/api/v1/transactions/$id/cancellation") {
                bearer(other)
                jsonBody("""{"occurredOn":"2026-10-04"}""")
            }
            assertEquals(400, cancellation.status.value, cancellation.bodyAsText())
        } finally {
            cleanupUser(ownerEmail)
            cleanupUser(otherEmail)
        }
    }

    private suspend fun registerAndLogin(client: HttpClient, email: String, password: String, displayName: String): String {
        val register = client.post("/api/v1/auth/register") { jsonBody("""{"email":"$email","displayName":"$displayName","password":"$password","timeZone":"UTC"}""") }
        assertEquals(201, register.status.value, register.bodyAsText())
        val login = client.post("/api/v1/auth/login") { jsonBody("""{"email":"$email","password":"$password"}""") }
        assertEquals(200, login.status.value, login.bodyAsText())
        return json(login.bodyAsText())["token"]!!.jsonPrimitive.content
    }

    private suspend fun createAccount(client: HttpClient, token: String, name: String, openingBalance: Long, type: String): String {
        val response = client.post("/api/v1/accounts") { bearer(token); jsonBody("""{"name":"$name","type":"$type","openingBalanceMinor":$openingBalance,"currency":"BRL"}""") }
        assertEquals(201, response.status.value, response.bodyAsText())
        return json(response.bodyAsText())["id"]!!.jsonPrimitive.content
    }

    private suspend fun createCard(client: HttpClient, token: String, liabilityAccountId: String, limit: Long): String {
        val response = client.post("/api/v1/cards") { bearer(token); jsonBody("""{"liabilityAccountId":"$liabilityAccountId","name":"Visa","brand":"VISA","lastFourDigits":"1234","creditLimitMinor":$limit,"currency":"BRL","closingDay":10,"dueDay":20}""") }
        assertEquals(201, response.status.value, response.bodyAsText())
        return response.bodyAsText()
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
                c.prepareStatement("DELETE FROM invoice_payment_transactions WHERE invoice_id IN (SELECT id FROM invoices WHERE card_id IN (SELECT id FROM cards WHERE owner_id = ?))").use { s -> s.setObject(1, userId); s.executeUpdate() }
                c.prepareStatement("UPDATE transactions SET related_transaction_id = NULL WHERE account_id IN (SELECT id FROM accounts WHERE owner_id = ?)").use { s -> s.setObject(1, userId); s.executeUpdate() }
                c.prepareStatement("DELETE FROM transactions WHERE account_id IN (SELECT id FROM accounts WHERE owner_id = ?)").use { s -> s.setObject(1, userId); s.executeUpdate() }
                c.prepareStatement("DELETE FROM invoices WHERE card_id IN (SELECT id FROM cards WHERE owner_id = ?)").use { s -> s.setObject(1, userId); s.executeUpdate() }
                c.prepareStatement("DELETE FROM cards WHERE owner_id = ?").use { s -> s.setObject(1, userId); s.executeUpdate() }
                c.prepareStatement("DELETE FROM accounts WHERE owner_id = ?").use { s -> s.setObject(1, userId); s.executeUpdate() }
                c.prepareStatement("DELETE FROM users WHERE id = ?").use { s -> s.setObject(1, userId); s.executeUpdate() }
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
