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

class CardInvoiceApiTest {
    @Test
    fun cardInvoicePaymentLifecycleAgainstPostgres() = testApplication {
        assumeTrue(hasPostgres())
        val email = uniqueEmail("card-lifecycle")
        application { module() }
        try {
            assertEquals(401, client.get("/api/v1/cards").status.value)
            val token = registerAndLogin(client, email, "password-123", "Card User")
            val cash = createAccount(client, token, "Checking", 200_000, "CHECKING")
            val liability = createAccount(client, token, "Visa Liability", 0, "CREDIT_CARD")
            val card = createCard(client, token, liability, 100_000)
            val cardId = json(card)["id"]!!.jsonPrimitive.content

            val purchase = client.post("/api/v1/cards/$cardId/purchases") {
                bearer(token)
                jsonBody("""{"amountMinor":60000,"currency":"BRL","occurredOn":"2026-10-03","description":"Laptop","post":true}""")
            }
            assertEquals(201, purchase.status.value, purchase.bodyAsText())
            assertTrue(purchase.bodyAsText().contains(""""status":"POSTED""""), purchase.bodyAsText())

            val cardAfterPurchase = client.get("/api/v1/cards/$cardId") { bearer(token) }
            assertEquals(40_000L, json(cardAfterPurchase.bodyAsText())["availableLimitMinor"]!!.jsonPrimitive.content.toLong())

            val invoice = client.post("/api/v1/invoices") {
                bearer(token)
                jsonBody("""{"cardId":"$cardId","periodStart":"2026-10-01","periodEnd":"2026-10-31","dueOn":"2026-11-10"}""")
            }
            assertEquals(201, invoice.status.value, invoice.bodyAsText())
            val invoiceId = json(invoice.bodyAsText())["id"]!!.jsonPrimitive.content
            assertEquals(60_000L, json(invoice.bodyAsText())["totalAmountMinor"]!!.jsonPrimitive.content.toLong())

            assertEquals(200, client.post("/api/v1/invoices/$invoiceId/close") { bearer(token) }.status.value)
            val payment = client.post("/api/v1/invoices/$invoiceId/payments") {
                bearer(token)
                jsonBody("""{"sourceAccountId":"$cash","amountMinor":60000,"currency":"BRL","occurredOn":"2026-11-10"}""")
            }
            assertEquals(200, payment.status.value, payment.bodyAsText())
            assertTrue(payment.bodyAsText().contains(""""status":"PAID""""), payment.bodyAsText())
            assertEquals(0L, json(payment.bodyAsText())["remainingAmountMinor"]!!.jsonPrimitive.content.toLong())

            val cashAfter = client.get("/api/v1/accounts/$cash") { bearer(token) }
            assertEquals(140_000L, json(cashAfter.bodyAsText())["balanceMinor"]!!.jsonPrimitive.content.toLong())
            val cardAfter = client.get("/api/v1/cards/$cardId") { bearer(token) }
            assertEquals(100_000L, json(cardAfter.bodyAsText())["availableLimitMinor"]!!.jsonPrimitive.content.toLong())
        } finally {
            cleanupUser(email)
        }
    }

    @Test
    fun invoiceSupportsPartialPaymentAndUserIsolation() = testApplication {
        assumeTrue(hasPostgres())
        val ownerEmail = uniqueEmail("card-owner")
        val otherEmail = uniqueEmail("card-other")
        application { module() }
        try {
            val owner = registerAndLogin(client, ownerEmail, "password-123", "Owner")
            val other = registerAndLogin(client, otherEmail, "password-123", "Other")
            val cash = createAccount(client, owner, "Checking", 100_000, "CHECKING")
            val liability = createAccount(client, owner, "Liability", 0, "CREDIT_CARD")
            val card = json(createCard(client, owner, liability, 100_000))["id"]!!.jsonPrimitive.content
            val purchase = client.post("/api/v1/cards/$card/purchases") { bearer(owner); jsonBody("""{"amountMinor":50000,"currency":"BRL","occurredOn":"2026-10-03","post":true}""") }
            assertEquals(201, purchase.status.value, purchase.bodyAsText())
            val invoice = client.post("/api/v1/invoices") { bearer(owner); jsonBody("""{"cardId":"$card","periodStart":"2026-10-01","periodEnd":"2026-10-31","dueOn":"2026-11-10"}""") }
            assertEquals(201, invoice.status.value, invoice.bodyAsText())
            val invoiceId = json(invoice.bodyAsText())["id"]!!.jsonPrimitive.content
            assertEquals(400, client.get("/api/v1/invoices/$invoiceId") { bearer(other) }.status.value)
            assertEquals(200, client.post("/api/v1/invoices/$invoiceId/close") { bearer(owner) }.status.value)

            val partial = client.post("/api/v1/invoices/$invoiceId/payments") { bearer(owner); jsonBody("""{"sourceAccountId":"$cash","amountMinor":20000,"currency":"BRL","occurredOn":"2026-11-10"}""") }
            assertEquals(200, partial.status.value, partial.bodyAsText())
            assertTrue(partial.bodyAsText().contains(""""status":"CLOSED""""), partial.bodyAsText())
            assertEquals(30_000L, json(partial.bodyAsText())["remainingAmountMinor"]!!.jsonPrimitive.content.toLong())

            val final = client.post("/api/v1/invoices/$invoiceId/payments") { bearer(owner); jsonBody("""{"sourceAccountId":"$cash","amountMinor":30000,"currency":"BRL","occurredOn":"2026-11-11"}""") }
            assertEquals(200, final.status.value, final.bodyAsText())
            assertTrue(final.bodyAsText().contains(""""status":"PAID""""), final.bodyAsText())
        } finally {
            cleanupUser(ownerEmail)
            cleanupUser(otherEmail)
        }
    }

    @Test
    fun cardPurchaseRejectsCreditLimitOverflow() = testApplication {
        assumeTrue(hasPostgres())
        val email = uniqueEmail("card-limit")
        application { module() }
        try {
            val token = registerAndLogin(client, email, "password-123", "Limit User")
            val liability = createAccount(client, token, "Liability", 0, "CREDIT_CARD")
            val card = json(createCard(client, token, liability, 100_000))["id"]!!.jsonPrimitive.content
            val rejected = client.post("/api/v1/cards/$card/purchases") { bearer(token); jsonBody("""{"amountMinor":100001,"currency":"BRL","occurredOn":"2026-10-03"}""") }
            assertEquals(400, rejected.status.value, rejected.bodyAsText())
        } finally {
            cleanupUser(email)
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

    private fun io.ktor.client.request.HttpRequestBuilder.jsonBody(body: String) { header(HttpHeaders.ContentType, ContentType.Application.Json.toString()); setBody(body) }
    private fun io.ktor.client.request.HttpRequestBuilder.bearer(token: String) { header(HttpHeaders.Authorization, "Bearer $token") }
    private fun json(body: String) = Json.parseToJsonElement(body).jsonObject
    private fun uniqueEmail(prefix: String): String = prefix + "-" + UUID.randomUUID() + "@example.com"
    private fun hasPostgres(): Boolean = !System.getenv("POSTGRES_PASSWORD").isNullOrBlank()

    private fun cleanupUser(email: String) {
        DatabaseConnectionProvider().open().use { c ->
            val userId = c.prepareStatement("SELECT id FROM users WHERE email = ?").use { s -> s.setString(1, email); s.executeQuery().use { r -> if (r.next()) r.getObject("id", UUID::class.java) else null } } ?: return
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
            } catch (e: Exception) { runCatching { c.rollback() }; throw e } finally { c.autoCommit = true }
        }
    }
}