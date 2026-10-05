package com.nexa.finance

import com.nexa.finance.application.openfinance.OpenFinanceProvider
import com.nexa.finance.application.openfinance.OpenFinanceProviderRegistry
import com.nexa.finance.domain.common.UserId
import com.nexa.finance.infrastructure.database.DatabaseConnectionProvider
import io.ktor.client.HttpClient
import io.ktor.client.request.delete
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
import java.time.Instant
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds
import org.junit.jupiter.api.Assumptions.assumeTrue

class ApplicationTest {
    @Test
    fun authenticationApiLifecycleWorksAgainstPostgres() = testApplication {
        assumeTrue(hasPostgres())
        val email = uniqueEmail("api")
        val password = "api-password"

        application { module() }

        try {
            val token = registerAndLogin(client, email, password, "API User")

            val me = client.get("/api/v1/auth/me") {
                header(HttpHeaders.Authorization, "Bearer $token")
            }
            assertEquals(200, me.status.value)
            assertTrue(me.bodyAsText().contains("API User"))

            val logout = client.post("/api/v1/auth/logout") {
                header(HttpHeaders.Authorization, "Bearer $token")
            }
            assertEquals(204, logout.status.value)

            val afterLogout = client.get("/api/v1/auth/me") {
                header(HttpHeaders.Authorization, "Bearer $token")
            }
            assertEquals(401, afterLogout.status.value)
        } finally {
            cleanupUsers(email)
        }
    }

    @Test
    fun recoveryApiResetsPasswordAndInvalidatesExistingSession() = testApplication {
        assumeTrue(hasPostgres())
        val email = uniqueEmail("recovery")
        val oldPassword = "old-password"
        val newPassword = "new-password"

        application { module() }

        try {
            val oldToken = registerAndLogin(client, email, oldPassword, "Recovery User")

            val request = client.post("/api/v1/auth/recovery/request") {
                jsonBody("""{"email":"$email"}""")
            }
            assertEquals(200, request.status.value)
            val recoveryToken = json(request.bodyAsText())["token"]!!.jsonPrimitive.content

            val reset = client.post("/api/v1/auth/recovery/reset") {
                jsonBody("""{"token":"$recoveryToken","newPassword":"$newPassword"}""")
            }
            assertEquals(204, reset.status.value)

            val oldSession = client.get("/api/v1/auth/me") {
                header(HttpHeaders.Authorization, "Bearer $oldToken")
            }
            assertEquals(401, oldSession.status.value)

            val oldLogin = client.post("/api/v1/auth/login") {
                jsonBody("""{"email":"$email","password":"$oldPassword"}""")
            }
            assertEquals(400, oldLogin.status.value)

            val newLogin = client.post("/api/v1/auth/login") {
                jsonBody("""{"email":"$email","password":"$newPassword"}""")
            }
            assertEquals(200, newLogin.status.value)

            val secondReset = client.post("/api/v1/auth/recovery/reset") {
                jsonBody("""{"token":"$recoveryToken","newPassword":"another-password"}""")
            }
            assertEquals(400, secondReset.status.value)
        } finally {
            cleanupUsers(email)
        }
    }

    @Test
    fun profileApiUpdatesFields() = testApplication {
        assumeTrue(hasPostgres())
        val email = uniqueEmail("profile")

        application { module() }

        try {
            val token = registerAndLogin(client, email, "profile-password", "Before")

            val update = client.put("/api/v1/auth/me") {
                header(HttpHeaders.Authorization, "Bearer $token")
                jsonBody("""{"displayName":"After","timeZone":"America/Sao_Paulo"}""")
            }
            assertEquals(200, update.status.value)
            assertTrue(update.bodyAsText().contains("After"))
            assertTrue(update.bodyAsText().contains("America/Sao_Paulo"))

            val me = client.get("/api/v1/auth/me") {
                header(HttpHeaders.Authorization, "Bearer $token")
            }
            assertEquals(200, me.status.value)
            assertTrue(me.bodyAsText().contains("After"))
            assertTrue(me.bodyAsText().contains("America/Sao_Paulo"))

            val export = client.get("/api/v1/auth/me/export") {
                header(HttpHeaders.Authorization, "Bearer $token")
            }
            assertEquals(200, export.status.value)
            assertTrue(export.headers[HttpHeaders.ContentDisposition]?.contains("nexa-data-export.json") == true)
            assertTrue(export.bodyAsText().contains(email))
            assertFalse(export.bodyAsText().contains("password_hash"))
        } finally {
            cleanupUsers(email)
        }
    }

    @Test
    fun accountApiEnforcesUserIsolationAgainstPostgres() = testApplication {
        assumeTrue(hasPostgres())
        val ownerEmail = uniqueEmail("owner")
        val otherEmail = uniqueEmail("other")

        application { module() }

        try {
            val ownerToken = registerAndLogin(client, ownerEmail, "owner-password", "Owner")
            val otherToken = registerAndLogin(client, otherEmail, "other-password", "Other")

            val create = client.post("/api/v1/accounts") {
                header(HttpHeaders.Authorization, "Bearer $ownerToken")
                jsonBody("""{"name":"NEXA Checking","type":"CHECKING","openingBalanceMinor":100000,"currency":"BRL"}""")
            }
            assertEquals(201, create.status.value)
            val accountId = json(create.bodyAsText())["id"]!!.jsonPrimitive.content

            val ownerList = client.get("/api/v1/accounts") {
                header(HttpHeaders.Authorization, "Bearer $ownerToken")
            }
            assertEquals(200, ownerList.status.value)
            assertTrue(ownerList.bodyAsText().contains(accountId))

            val otherList = client.get("/api/v1/accounts") {
                header(HttpHeaders.Authorization, "Bearer $otherToken")
            }
            assertEquals(200, otherList.status.value)
            assertFalse(otherList.bodyAsText().contains(accountId))

            val crossUser = client.get("/api/v1/accounts/$accountId") {
                header(HttpHeaders.Authorization, "Bearer $otherToken")
            }
            assertEquals(400, crossUser.status.value)

            val unauthenticated = client.get("/api/v1/accounts")
            assertEquals(401, unauthenticated.status.value)
        } finally {
            cleanupUsers(ownerEmail, otherEmail)
        }
    }

    @Test
    fun openFinanceConsentApiCreatesListsAndRevokesWithUserIsolation() = testApplication {
        assumeTrue(hasPostgres())
        val ownerEmail = uniqueEmail("open-finance-owner")
        val otherEmail = uniqueEmail("open-finance-other")

        application { module() }

        try {
            val ownerToken = registerAndLogin(client, ownerEmail, "owner-password", "Open Finance Owner")
            val otherToken = registerAndLogin(client, otherEmail, "other-password", "Open Finance Other")

            val create = client.post("/api/v1/open-finance/consents") {
                header(HttpHeaders.Authorization, "Bearer $ownerToken")
                jsonBody(
                    """{"providerId":"belvo","scopes":["ACCOUNTS","BALANCES","TRANSACTIONS"]}"""
                )
            }
            assertEquals(200, create.status.value, create.bodyAsText())
            assertTrue(create.bodyAsText().contains("\"providerId\":\"belvo\""))
            assertTrue(create.bodyAsText().contains("\"status\":\"REQUESTED\""))
            val consentId = json(create.bodyAsText())["id"]!!.jsonPrimitive.content

            val ownerList = client.get("/api/v1/open-finance/consents") {
                header(HttpHeaders.Authorization, "Bearer $ownerToken")
            }
            assertEquals(200, ownerList.status.value)
            assertTrue(ownerList.bodyAsText().contains(consentId))

            val otherList = client.get("/api/v1/open-finance/consents") {
                header(HttpHeaders.Authorization, "Bearer $otherToken")
            }
            assertEquals(200, otherList.status.value)
            assertFalse(otherList.bodyAsText().contains(consentId))

            val revoke = client.delete("/api/v1/open-finance/consents/$consentId") {
                header(HttpHeaders.Authorization, "Bearer $ownerToken")
            }
            assertEquals(200, revoke.status.value, revoke.bodyAsText())
            assertTrue(revoke.bodyAsText().contains("\"status\":\"REVOKED\""))

            val revokedList = client.get("/api/v1/open-finance/consents") {
                header(HttpHeaders.Authorization, "Bearer $ownerToken")
            }
            assertEquals(200, revokedList.status.value)
            assertTrue(revokedList.bodyAsText().contains("\"status\":\"REVOKED\""))
        } finally {
            cleanupOpenFinanceConsents(ownerEmail, otherEmail)
            cleanupUsers(ownerEmail, otherEmail)
        }
    }

    @Test
    fun openFinanceAuthorizationSyncAndRevokeUseInjectedProvider() = testApplication {
        assumeTrue(hasPostgres())
        val email = uniqueEmail("open-finance-provider")
        val provider = FakeOpenFinanceProvider()

        application {
            module(FakeOpenFinanceProviderRegistry(provider))
        }

        try {
            val token = registerAndLogin(client, email, "provider-password", "Provider User")

            val create = client.post("/api/v1/open-finance/consents") {
                header(HttpHeaders.Authorization, "Bearer $token")
                jsonBody("""{"providerId":"fake","scopes":["ACCOUNTS","BALANCES","TRANSACTIONS"]}""")
            }
            assertEquals(200, create.status.value, create.bodyAsText())
            val consentId = json(create.bodyAsText())["id"]!!.jsonPrimitive.content

            val authorization = client.post("/api/v1/open-finance/consents/$consentId/authorization") {
                header(HttpHeaders.Authorization, "Bearer $token")
            }
            assertEquals(200, authorization.status.value, authorization.bodyAsText())
            assertTrue(authorization.bodyAsText().contains(""""externalConsentId":"${provider.externalConsentId}""""))
            assertTrue(authorization.bodyAsText().contains(""""authorizationUrl":"${provider.authorizationUrl}""""))

            val awaiting = client.get("/api/v1/open-finance/consents") {
                header(HttpHeaders.Authorization, "Bearer $token")
            }
            assertEquals(200, awaiting.status.value)
            assertTrue(awaiting.bodyAsText().contains(""""status":"AWAITING_AUTHORIZATION""""))

            provider.activate()

            val sync = client.post("/api/v1/open-finance/consents/$consentId/sync") {
                header(HttpHeaders.Authorization, "Bearer $token")
            }
            assertEquals(200, sync.status.value, sync.bodyAsText())
            assertTrue(sync.bodyAsText().contains(""""status":"ACTIVE""""))
            assertTrue(sync.bodyAsText().contains(""""expiresAt":""""))

            val revoke = client.delete("/api/v1/open-finance/consents/$consentId") {
                header(HttpHeaders.Authorization, "Bearer $token")
            }
            assertEquals(200, revoke.status.value, revoke.bodyAsText())
            assertTrue(revoke.bodyAsText().contains(""""status":"REVOKED""""))
            assertTrue(provider.revoked)
        } finally {
            cleanupOpenFinanceConsents(email)
            cleanupUsers(email)
        }
    }
    @Test
    fun goalApiCreatesContributesAndReturnsHistoryAgainstPostgres() = testApplication {
        assumeTrue(hasPostgres())
        val email = uniqueEmail("goal-api")
        application { module() }

        try {
            val token = registerAndLogin(client, email, "goal-password", "Goal API User")

            val create = client.post("/api/v1/goals") {
                header(HttpHeaders.Authorization, "Bearer $token")
                jsonBody(
                    """{"name":"Viagem","targetAmountMinor":10000,"currency":"BRL","targetDate":"2027-01-15","description":"Férias"}"""
                )
            }
            assertEquals(201, create.status.value)
            val goalId = json(create.bodyAsText())["id"]!!.jsonPrimitive.content
            assertTrue(create.bodyAsText().contains("\"progressPercent\":0"))

            val list = client.get("/api/v1/goals") {
                header(HttpHeaders.Authorization, "Bearer $token")
            }
            assertEquals(200, list.status.value)
            assertTrue(list.bodyAsText().contains(goalId))

            val contribution = client.post("/api/v1/goals/$goalId/contributions") {
                header(HttpHeaders.Authorization, "Bearer $token")
                jsonBody("""{"amountMinor":2500,"currency":"BRL","occurredOn":"2026-10-04"}""")
            }
            assertEquals(200, contribution.status.value)
            assertTrue(contribution.bodyAsText().contains("\"currentAmountMinor\":2500"))
            assertTrue(contribution.bodyAsText().contains("\"status\":\"ACTIVE\""))
            assertTrue(contribution.bodyAsText().contains("\"amountMinor\":2500"))

            val detail = client.get("/api/v1/goals/$goalId") {
                header(HttpHeaders.Authorization, "Bearer $token")
            }
            assertEquals(200, detail.status.value)
            assertTrue(detail.bodyAsText().contains("\"status\":\"ACTIVE\""))
            assertTrue(detail.bodyAsText().contains("\"contributions\""))

            val pause = client.post("/api/v1/goals/$goalId/pause") {
                header(HttpHeaders.Authorization, "Bearer $token")
            }
            assertEquals(200, pause.status.value)
            assertTrue(pause.bodyAsText().contains("\"status\":\"PAUSED\""))

            val resume = client.post("/api/v1/goals/$goalId/resume") {
                header(HttpHeaders.Authorization, "Bearer $token")
            }
            assertEquals(200, resume.status.value)
            assertTrue(resume.bodyAsText().contains("\"status\":\"ACTIVE\""))

            val cancel = client.delete("/api/v1/goals/$goalId") {
                header(HttpHeaders.Authorization, "Bearer $token")
            }
            assertEquals(200, cancel.status.value)
            assertTrue(cancel.bodyAsText().contains("\"status\":\"CANCELLED\""))

            val cancelledDetail = client.get("/api/v1/goals/$goalId") {
                header(HttpHeaders.Authorization, "Bearer $token")
            }
            assertEquals(200, cancelledDetail.status.value)
            assertTrue(cancelledDetail.bodyAsText().contains("\"status\":\"CANCELLED\""))
        } finally {
            cleanupGoalsByEmail(email)
            cleanupUsers(email)
        }
    }

    @Test
    fun invalidJsonReturnsStandardApiError() = testApplication {
        application { module() }

        val response = client.post("/api/v1/auth/login") {
            jsonBody("""{}""")
        }

        assertEquals(400, response.status.value)
        assertTrue(response.bodyAsText().contains("VALIDATION_ERROR"))
    }

    @Test
    fun healthEndpointReturnsOk() = testApplication {
        application { module() }

        val response = client.get("/health")
        assertEquals(200, response.status.value)
        assertEquals("NEXA Finance API - OK", response.bodyAsText().trim())
    }

    @Test
    fun serverSettingsUseDevelopmentDefaults() {
        val settings = ServerSettings.fromEnvironment(emptyMap())

        assertEquals("0.0.0.0", settings.host)
        assertEquals(8080, settings.port)
        assertEquals(15_000L, settings.requestTimeoutMillis)
    }

    @Test
    fun serverSettingsReadRuntimeEnvironment() {
        val settings = ServerSettings.fromEnvironment(
            mapOf(
                "SERVER_HOST" to "127.0.0.1",
                "SERVER_PORT" to "9090",
                "SERVER_REQUEST_TIMEOUT_MS" to "30000",
            )
        )

        assertEquals("127.0.0.1", settings.host)
        assertEquals(9090, settings.port)
        assertEquals(30_000L, settings.requestTimeoutMillis)
    }

    @Test
    fun serverSettingsRejectInvalidPort() {
        val exception = kotlin.test.assertFailsWith<IllegalArgumentException> {
            ServerSettings.fromEnvironment(mapOf("SERVER_PORT" to "70000"))
        }
        assertTrue(exception.message!!.contains("SERVER_PORT"))
    }

    @Test
    fun serverSettingsRejectInvalidRequestTimeout() {
        val exception = kotlin.test.assertFailsWith<IllegalArgumentException> {
            ServerSettings.fromEnvironment(mapOf("SERVER_REQUEST_TIMEOUT_MS" to "999"))
        }
        assertTrue(exception.message!!.contains("SERVER_REQUEST_TIMEOUT_MS"))
    }

    @Test
    fun rateLimitSettingsUseSafeDefaults() {
        val settings = RateLimitSettings.fromEnvironment(emptyMap())

        assertEquals(120, settings.requestsPerMinute)
        assertEquals(60.seconds, settings.refillPeriod)
    }

    @Test
    fun rateLimitSettingsReadRuntimeEnvironment() {
        val settings = RateLimitSettings.fromEnvironment(
            mapOf(
                "RATE_LIMIT_REQUESTS_PER_MINUTE" to "250",
                "RATE_LIMIT_WINDOW_SECONDS" to "30",
            )
        )

        assertEquals(250, settings.requestsPerMinute)
        assertEquals(30.seconds, settings.refillPeriod)
    }

    @Test
    fun rateLimitSettingsRejectInvalidConfiguration() {
        val exception = kotlin.test.assertFailsWith<IllegalArgumentException> {
            RateLimitSettings.fromEnvironment(mapOf("RATE_LIMIT_REQUESTS_PER_MINUTE" to "0"))
        }
        assertTrue(exception.message!!.contains("RATE_LIMIT_REQUESTS_PER_MINUTE"))
    }

    @Test
    fun apiRateLimitIsScopedPerClientAndReturnsStandardError() = testApplication {
        application { module() }

        repeat(120) {
            val response = client.get("/api/v1/goals") {
                header("X-Forwarded-For", "rate-limit-test-client")
            }
            assertEquals(401, response.status.value)
        }

        val limited = client.get("/api/v1/goals") {
            header("X-Forwarded-For", "rate-limit-test-client")
        }

        assertEquals(429, limited.status.value)
        assertTrue(limited.headers["Retry-After"] != null)
        assertTrue(limited.bodyAsText().contains("RATE_LIMITED"))

        val otherClient = client.get("/api/v1/goals") {
            header("X-Forwarded-For", "different-client")
        }
        assertEquals(401, otherClient.status.value)
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

    private fun json(body: String) = Json.parseToJsonElement(body).jsonObject

    private fun uniqueEmail(prefix: String): String =
        prefix + "-" + UUID.randomUUID() + "@example.com"

    private fun hasPostgres(): Boolean = !System.getenv("POSTGRES_PASSWORD").isNullOrBlank()

    private fun cleanupGoalsByEmail(email: String) {
        DatabaseConnectionProvider().open().use { connection ->
            connection.prepareStatement("DELETE FROM goals WHERE user_id IN (SELECT id FROM users WHERE email = ?)").use { statement ->
                statement.setString(1, email)
                statement.executeUpdate()
            }
        }
    }

    
private class FakeOpenFinanceProviderRegistry(
    private val provider: OpenFinanceProvider,
) : OpenFinanceProviderRegistry {
    override fun get(providerId: String): OpenFinanceProvider =
        checkProvider(providerId)

    private fun checkProvider(providerId: String): OpenFinanceProvider {
        check(providerId == provider.providerId) { "Unexpected provider: " + providerId }
        return provider
    }
}

private class FakeOpenFinanceProvider : OpenFinanceProvider {
    override val providerId: String = "fake"

    val externalConsentId: String = "fake-consent-1"
    val authorizationUrl: String = "https://sandbox.example/open-finance/authorize/fake-consent-1"
    var status: OpenFinanceProvider.ExternalConsentStatus =
        OpenFinanceProvider.ExternalConsentStatus.AWAITING_AUTHORIZATION
    var expiresAt: Instant? = null
    var revoked: Boolean = false

    override fun requestConsent(
        userId: UserId,
        scopes: Set<String>,
    ): OpenFinanceProvider.ProviderConsentSession =
        OpenFinanceProvider.ProviderConsentSession(
            externalConsentId = externalConsentId,
            authorizationUrl = authorizationUrl,
        )

    override fun getConsentStatus(
        externalConsentId: String,
    ): OpenFinanceProvider.ProviderConsentState {
        check(externalConsentId == this.externalConsentId)
        return OpenFinanceProvider.ProviderConsentState(
            status = status,
            expiresAt = expiresAt,
        )
    }

    override fun revokeConsent(
        externalConsentId: String,
    ) {
        check(externalConsentId == this.externalConsentId)
        status = OpenFinanceProvider.ExternalConsentStatus.REVOKED
        revoked = true
    }

    fun activate() {
        status = OpenFinanceProvider.ExternalConsentStatus.ACTIVE
        expiresAt = Instant.now().plusSeconds(3_600)
    }
}

private fun cleanupOpenFinanceConsents(vararg emails: String) {
        DatabaseConnectionProvider().open().use { connection ->
            connection.prepareStatement(
                "DELETE FROM open_finance_consents WHERE user_id IN (SELECT id FROM users WHERE email = ANY (?))"
            ).use { statement ->
                statement.setArray(1, connection.createArrayOf("text", emails))
                statement.executeUpdate()
            }
        }
    }

    private fun cleanupUsers(vararg emails: String) {
        DatabaseConnectionProvider().open().use { connection ->
            connection.autoCommit = false
            try {
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
