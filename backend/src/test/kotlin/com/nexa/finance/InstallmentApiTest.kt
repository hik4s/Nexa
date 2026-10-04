package com.nexa.finance

import com.nexa.finance.infrastructure.database.DatabaseConnectionProvider
import io.ktor.client.HttpClient
import io.ktor.client.request.*
import io.ktor.client.statement.bodyAsText
import io.ktor.http.*
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Assumptions.assumeTrue
import java.util.UUID
import kotlin.test.*

class InstallmentApiTest {
    @Test
    fun createsSplitsCompletesAndCancelsInstallmentPlanAgainstPostgres() = testApplication {
        assumeTrue(hasPostgres())
        val email = uniqueEmail("installment")
        application { module() }
        try {
            val token = registerAndLogin(client, email, "password-123", "Installment User")
            val created = client.post("/api/v1/installments") {
                bearer(token)
                jsonBody("""{"totalAmountMinor":10000,"currency":"BRL","count":3,"firstDueOn":"2026-01-31","description":"Notebook"}""")
            }
            assertEquals(201, created.status.value, created.bodyAsText())
            val body = json(created.bodyAsText())
            val id = body["id"]!!.jsonPrimitive.content
            val items = body["items"]!!.jsonArray
            assertEquals(3, items.size)
            assertEquals(3334L, items[0].jsonObject["amountMinor"]!!.jsonPrimitive.content.toLong())
            assertEquals(3333L, items[1].jsonObject["amountMinor"]!!.jsonPrimitive.content.toLong())
            assertEquals(3333L, items[2].jsonObject["amountMinor"]!!.jsonPrimitive.content.toLong())
            assertEquals("2026-01-31", items[0].jsonObject["dueOn"]!!.jsonPrimitive.content)
            assertEquals("2026-02-28", items[1].jsonObject["dueOn"]!!.jsonPrimitive.content)
            assertEquals("2026-03-31", items[2].jsonObject["dueOn"]!!.jsonPrimitive.content)

            assertEquals(200, client.post("/api/v1/installments/" + id + "/items/1/complete") { bearer(token) }.status.value)
            assertEquals(200, client.post("/api/v1/installments/" + id + "/items/2/complete") { bearer(token) }.status.value)
            val completed = client.post("/api/v1/installments/" + id + "/items/3/complete") { bearer(token) }
            assertEquals(200, completed.status.value, completed.bodyAsText())
            assertEquals("COMPLETED", json(completed.bodyAsText())["status"]!!.jsonPrimitive.content)
        } finally { cleanupUser(email) }
    }

    @Test
    fun cancellationOwnershipAndInvalidBoundsWork() = testApplication {
        assumeTrue(hasPostgres())
        val ownerEmail = uniqueEmail("installment-owner")
        val otherEmail = uniqueEmail("installment-other")
        application { module() }
        try {
            val owner = registerAndLogin(client, ownerEmail, "password-123", "Owner")
            val other = registerAndLogin(client, otherEmail, "password-123", "Other")
            val created = client.post("/api/v1/installments") {
                bearer(owner)
                jsonBody("""{"totalAmountMinor":9000,"currency":"USD","count":3,"firstDueOn":"2026-10-03"}""")
            }
            assertEquals(201, created.status.value, created.bodyAsText())
            val id = json(created.bodyAsText())["id"]!!.jsonPrimitive.content
            assertEquals(400, client.get("/api/v1/installments/" + id) { bearer(other) }.status.value)
            assertEquals(200, client.post("/api/v1/installments/" + id + "/cancel") { bearer(owner) }.status.value)
            assertEquals(400, client.post("/api/v1/installments/" + id + "/cancel") { bearer(owner) }.status.value)

            assertEquals(400, client.post("/api/v1/installments") {
                bearer(owner)
                jsonBody("""{"totalAmountMinor":1000,"currency":"BRL","count":1,"firstDueOn":"2026-10-03"}""")
            }.status.value)
            assertEquals(400, client.post("/api/v1/installments") {
                bearer(owner)
                jsonBody("""{"totalAmountMinor":1000,"currency":"GBP","count":2,"firstDueOn":"2026-10-03"}""")
            }.status.value)
        } finally { cleanupUser(ownerEmail); cleanupUser(otherEmail) }
    }

    private suspend fun registerAndLogin(client: HttpClient, email: String, password: String, name: String): String {
        assertEquals(201, client.post("/api/v1/auth/register") {
            jsonBody("""{"email":"$email","displayName":"$name","password":"$password","timeZone":"UTC"}""")
        }.status.value)
        val login = client.post("/api/v1/auth/login") { jsonBody("""{"email":"$email","password":"$password"}""") }
        assertEquals(200, login.status.value, login.bodyAsText())
        return json(login.bodyAsText())["token"]!!.jsonPrimitive.content
    }

    private fun io.ktor.client.request.HttpRequestBuilder.jsonBody(body: String) {
        header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
        setBody(body)
    }
    private fun io.ktor.client.request.HttpRequestBuilder.bearer(token: String) {
        header(HttpHeaders.Authorization, "Bearer " + token)
    }
    private fun json(body: String) = Json.parseToJsonElement(body).jsonObject
    private fun uniqueEmail(prefix: String) = prefix + "-" + UUID.randomUUID() + "@example.com"
    private fun hasPostgres() = !System.getenv("POSTGRES_PASSWORD").isNullOrBlank()

    private fun cleanupUser(email: String) {
        DatabaseConnectionProvider().open().use { c ->
            val userId = c.prepareStatement("SELECT id FROM users WHERE email = ?").use { s ->
                s.setString(1, email)
                s.executeQuery().use { r -> if (r.next()) r.getObject("id", UUID::class.java) else null }
            } ?: return
            c.autoCommit = false
            try {
                c.prepareStatement("DELETE FROM installment_items WHERE installment_id IN (SELECT id FROM installments WHERE user_id = ?)").use { s -> s.setObject(1, userId); s.executeUpdate() }
                c.prepareStatement("DELETE FROM installments WHERE user_id = ?").use { s -> s.setObject(1, userId); s.executeUpdate() }
                c.prepareStatement("DELETE FROM transactions WHERE account_id IN (SELECT id FROM accounts WHERE owner_id = ?)").use { s -> s.setObject(1, userId); s.executeUpdate() }
                c.prepareStatement("DELETE FROM recurrences WHERE user_id = ?").use { s -> s.setObject(1, userId); s.executeUpdate() }
                c.prepareStatement("DELETE FROM accounts WHERE owner_id = ?").use { s -> s.setObject(1, userId); s.executeUpdate() }
                c.prepareStatement("DELETE FROM users WHERE id = ?").use { s -> s.setObject(1, userId); s.executeUpdate() }
                c.commit()
            } catch (e: Exception) { c.rollback(); throw e } finally { c.autoCommit = true }
        }
    }
}
