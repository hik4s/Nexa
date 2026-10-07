package com.nexa.finance

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

class CategoryApiTest {
    @Test
    fun authenticatedUserCanListSystemCategoriesAndFilterByType() = testApplication {
        assumeTrue(!System.getenv("POSTGRES_PASSWORD").isNullOrBlank())
        application { module() }
        val email = uniqueEmail()
        try {
            val token = registerAndLogin(client, email)
            val all = client.get("/api/v1/categories") { bearer(token) }
            assertEquals(200, all.status.value, all.bodyAsText())
            val allJson = Json.parseToJsonElement(all.bodyAsText()).jsonArray
            assertTrue(allJson.size >= 10)
            assertTrue(allJson.any { it.jsonObject["name"]?.jsonPrimitive?.content == "Alimentação" })
            assertTrue(allJson.any { it.jsonObject["name"]?.jsonPrimitive?.content == "Outros" })

            val expenses = client.get("/api/v1/categories?type=EXPENSE") { bearer(token) }
            assertEquals(200, expenses.status.value, expenses.bodyAsText())
            val expenseJson = Json.parseToJsonElement(expenses.bodyAsText()).jsonArray
            assertTrue(expenseJson.any { it.jsonObject["name"]?.jsonPrimitive?.content == "Alimentação" })
            assertFalse(expenseJson.any { it.jsonObject["name"]?.jsonPrimitive?.content == "Salário" })

            val income = client.get("/api/v1/categories?type=INCOME") { bearer(token) }
            assertEquals(200, income.status.value, income.bodyAsText())
            val incomeJson = Json.parseToJsonElement(income.bodyAsText()).jsonArray
            assertTrue(incomeJson.any { it.jsonObject["name"]?.jsonPrimitive?.content == "Salário" })
            assertTrue(incomeJson.any { it.jsonObject["name"]?.jsonPrimitive?.content == "Outros" })
        } finally {
            cleanupUser(email)
        }
    }

    @Test
    fun categoriesRequireAuthentication() = testApplication {
        application { module() }
        assertEquals(401, client.get("/api/v1/categories").status.value)
    }

    private suspend fun registerAndLogin(client: HttpClient, email: String): String {
        assertEquals(
            201,
            client.post("/api/v1/auth/register") {
                jsonBody("""{"email":"$email","displayName":"Category User","password":"password-123","timeZone":"UTC"}""")
            }.status.value
        )
        val login = client.post("/api/v1/auth/login") {
            jsonBody("""{"email":"$email","password":"password-123"}""")
        }
        assertEquals(200, login.status.value, login.bodyAsText())
        return Json.parseToJsonElement(login.bodyAsText()).jsonObject["token"]!!.jsonPrimitive.content
    }

    private fun io.ktor.client.request.HttpRequestBuilder.jsonBody(body: String) {
        header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
        setBody(body)
    }

    private fun io.ktor.client.request.HttpRequestBuilder.bearer(token: String) {
        header(HttpHeaders.Authorization, "Bearer $token")
    }

    private fun uniqueEmail() = "category-" + UUID.randomUUID() + "@example.com"

    private fun cleanupUser(email: String) {
        com.nexa.finance.infrastructure.database.DatabaseConnectionProvider().open().use { c ->
            val userId = c.prepareStatement("SELECT id FROM users WHERE email = ?").use { s ->
                s.setString(1, email)
                s.executeQuery().use { r -> if (r.next()) r.getObject("id", UUID::class.java) else null }
            } ?: return
            c.autoCommit = false
            try {
                c.prepareStatement("UPDATE transactions SET related_transaction_id = NULL WHERE account_id IN (SELECT id FROM accounts WHERE owner_id = ?)").use {
                    it.setObject(1, userId)
                    it.executeUpdate()
                }
                c.prepareStatement("DELETE FROM transactions WHERE account_id IN (SELECT id FROM accounts WHERE owner_id = ?)").use {
                    it.setObject(1, userId)
                    it.executeUpdate()
                }
                c.prepareStatement("DELETE FROM recurrences WHERE user_id = ?").use {
                    it.setObject(1, userId)
                    it.executeUpdate()
                }
                c.prepareStatement("DELETE FROM cards WHERE owner_id = ?").use {
                    it.setObject(1, userId)
                    it.executeUpdate()
                }
                c.prepareStatement("DELETE FROM accounts WHERE owner_id = ?").use {
                    it.setObject(1, userId)
                    it.executeUpdate()
                }
                c.prepareStatement("DELETE FROM users WHERE id = ?").use {
                    it.setObject(1, userId)
                    it.executeUpdate()
                }
                c.commit()
            } catch (e: Exception) {
                c.rollback()
                throw e
            } finally {
                c.autoCommit = true
            }
        }
    }
}
