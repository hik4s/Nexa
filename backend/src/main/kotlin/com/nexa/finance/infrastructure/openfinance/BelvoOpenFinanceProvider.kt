package com.nexa.finance.infrastructure.openfinance

import com.nexa.finance.application.openfinance.OpenFinanceProvider
import com.nexa.finance.domain.common.DomainValidationException
import com.nexa.finance.domain.common.UserId
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.IOException
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.Base64
import java.util.Locale

class BelvoOpenFinanceProvider(
    private val settings: BelvoOpenFinanceSettings,
    private val httpClient: HttpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .build(),
    private val clock: Clock = Clock.systemUTC(),
) : OpenFinanceProvider {
    override val providerId: String = "belvo"

    private val json = Json {
        ignoreUnknownKeys = true
    }

    override fun requestConsent(
        userId: UserId,
        scopes: Set<String>,
    ): OpenFinanceProvider.ProviderConsentSession =
        throw DomainValidationException(
            "Belvo uses a Hosted Widget authorization session. Use the authorization-session endpoint.",
        )

    override fun requestAuthorizationSession(
        userId: UserId,
        scopes: Set<String>,
        context: OpenFinanceProvider.ProviderAuthorizationContext,
    ): OpenFinanceProvider.ProviderConsentSession {
        settings.requireUsable()
        val documentType = context.documentType?.trim()?.uppercase(Locale.ROOT)
            ?: throw DomainValidationException("Open Finance document type is required for Belvo.")
        val documentNumber = context.documentNumber
            ?.filter(Char::isDigit)
            ?: throw DomainValidationException("Open Finance document number is required for Belvo.")
        val holderName = context.holderName?.trim()
            ?: throw DomainValidationException("Open Finance holder name is required for Belvo.")

        if (documentType !in setOf("CPF", "CNPJ")) {
            throw DomainValidationException("Belvo document type must be CPF or CNPJ.")
        }
        if (documentNumber.length !in setOf(11, 14)) {
            throw DomainValidationException("Belvo document number must contain 11 or 14 digits.")
        }
        if (holderName.isBlank() || holderName.length > 128) {
            throw DomainValidationException("Belvo holder name must contain between 1 and 128 characters.")
        }
        if (!Regex("""^[A-Za-z0-9_-]{3,256}$""").matches(context.externalId)) {
            throw DomainValidationException("Belvo external ID is invalid.")
        }

        val resources = resourcesFor(scopes)
        val requestBody = BelvoWidgetTokenRequest(
            id = settings.secretId!!,
            password = settings.secretPassword!!,
            externalId = context.externalId,
            scopes = "read_institutions,write_links,read_consents,write_consents,write_consent_callback,delete_consents",
            fetchResources = resources,
            staleIn = settings.staleIn,
            widget = BelvoWidget(
                openFinanceFeature = "consent_link_creation",
                callbackUrls = BelvoCallbackUrls(
                    success = settings.successCallbackUrl!!,
                    exit = settings.exitCallbackUrl!!,
                    event = settings.eventCallbackUrl!!,
                ),
                consent = BelvoWidgetConsent(
                    purpose = settings.purpose,
                    termsAndConditionsUrl = settings.termsAndConditionsUrl!!,
                    permissions = listOf("REGISTER", "ACCOUNTS", "CREDIT_CARDS", "CREDIT_OPERATIONS"),
                    identificationInfo = listOf(
                        BelvoIdentificationInfo(
                            type = documentType,
                            number = documentNumber,
                            name = holderName,
                        ),
                    ),
                ),
            ),
        )

        val response = execute(
            method = "POST",
            path = "/api/token/",
            body = json.encodeToString(BelvoWidgetTokenRequest.serializer(), requestBody),
            contentType = "application/json",
            basicAuth = BasicCredentials(settings.secretId!!, settings.secretPassword!!),
        )
        val token = decode<BelvoWidgetTokenResponse>(response)
        if (token.access.isBlank()) {
            throw IOException("Belvo returned an empty widget access token.")
        }

        return OpenFinanceProvider.ProviderConsentSession(
            externalConsentId = null,
            authorizationUrl = "https://widget.belvo.io/",
            widgetAccessToken = token.access,
            expiresAt = Instant.now(clock).plusSeconds(600),
        )
    }

    override fun getConsentStatus(
        externalConsentId: String,
    ): OpenFinanceProvider.ProviderConsentState {
        settings.requireUsable()
        val response = execute(
            method = "GET",
            path = "/api/links/${encodePathSegment(externalConsentId)}/",
            body = null,
            contentType = null,
            basicAuth = BasicCredentials(settings.secretId!!, settings.secretPassword!!),
        )
        val link = decode<BelvoLinkResponse>(response)
        return OpenFinanceProvider.ProviderConsentState(
            status = when (link.status.lowercase(Locale.ROOT)) {
                "valid" -> OpenFinanceProvider.ExternalConsentStatus.ACTIVE
                "unconfirmed", "token_required" ->
                    OpenFinanceProvider.ExternalConsentStatus.AWAITING_AUTHORIZATION
                "invalid" -> OpenFinanceProvider.ExternalConsentStatus.FAILED
                else -> OpenFinanceProvider.ExternalConsentStatus.FAILED
            },
            expiresAt = null,
        )
    }

    override fun revokeConsent(
        externalConsentId: String,
    ) {
        settings.requireUsable()
        execute(
            method = "DELETE",
            path = "/api/links/${encodePathSegment(externalConsentId)}/",
            body = null,
            contentType = null,
            basicAuth = BasicCredentials(settings.secretId!!, settings.secretPassword!!),
        )
    }

    private fun resourcesFor(scopes: Set<String>): List<String> {
        val normalized = scopes.map(String::uppercase).toSet()
        return buildList {
            if ("IDENTITY" in normalized) add("OWNERS")
            if ("ACCOUNTS" in normalized || "BALANCES" in normalized) add("ACCOUNTS")
            if ("TRANSACTIONS" in normalized) add("TRANSACTIONS")
            if ("CREDIT_CARDS" in normalized) add("BILLS")
        }.ifEmpty {
            listOf("ACCOUNTS")
        }
    }

    private inline fun <reified T> decode(body: String): T =
        runCatching { json.decodeFromString<T>(body) }
            .getOrElse { throw IOException("Belvo returned an unreadable response.") }

    private fun execute(
        method: String,
        path: String,
        body: String?,
        contentType: String?,
        basicAuth: BasicCredentials?,
    ): String {
        val builder = HttpRequest.newBuilder()
            .uri(URI.create(baseUrl() + path))
            .timeout(Duration.ofSeconds(20))
            .header("Accept", "application/json")

        contentType?.let { builder.header("Content-Type", it) }
        basicAuth?.let { credentials ->
            val raw = credentials.id + ":" + credentials.password
            builder.header(
                "Authorization",
                "Basic " + Base64.getEncoder().encodeToString(raw.toByteArray(Charsets.UTF_8)),
            )
        }

        val request = when (method) {
            "GET" -> builder.GET().build()
            "DELETE" -> builder.DELETE().build()
            "POST" -> builder.POST(
                HttpRequest.BodyPublishers.ofString(body ?: "")
            ).build()
            else -> throw IllegalArgumentException("Unsupported HTTP method.")
        }

        val response = try {
            httpClient.send(request, HttpResponse.BodyHandlers.ofString())
        } catch (error: IOException) {
            throw error
        } catch (error: InterruptedException) {
            Thread.currentThread().interrupt()
            throw IOException("Belvo request was interrupted.", error)
        }

        return when {
            response.statusCode() in 200..299 -> response.body()
            response.statusCode() >= 500 -> throw IOException(
                "Belvo provider temporarily unavailable (HTTP ${response.statusCode()})."
            )
            response.statusCode() == 408 || response.statusCode() == 429 -> throw IOException(
                "Belvo provider temporarily unavailable (HTTP ${response.statusCode()})."
            )
            else -> throw DomainValidationException(
                "Belvo rejected the Open Finance request (HTTP ${response.statusCode()}).",
            )
        }
    }

    private fun baseUrl(): String = settings.baseUrl.trimEnd('/')

    private fun encodePathSegment(value: String): String =
        value.trim().takeIf { it.isNotBlank() && it.length <= 255 }
            ?: throw DomainValidationException("Belvo external consent ID is invalid.")

    private data class BasicCredentials(
        val id: String,
        val password: String,
    )

    @Serializable
    private data class BelvoWidgetTokenRequest(
        val id: String,
        val password: String,
        @SerialName("external_id")
        val externalId: String,
        val scopes: String,
        @SerialName("fetch_resources")
        val fetchResources: List<String>,
        @SerialName("stale_in")
        val staleIn: String,
        val widget: BelvoWidget,
    )

    @Serializable
    private data class BelvoWidget(
        @SerialName("openfinance_feature")
        val openFinanceFeature: String,
        @SerialName("callback_urls")
        val callbackUrls: BelvoCallbackUrls,
        val consent: BelvoWidgetConsent,
    )

    @Serializable
    private data class BelvoCallbackUrls(
        val success: String,
        val exit: String,
        val event: String,
    )

    @Serializable
    private data class BelvoWidgetConsent(
        val purpose: String,
        @SerialName("terms_and_conditions_url")
        val termsAndConditionsUrl: String,
        val permissions: List<String>,
        @SerialName("identification_info")
        val identificationInfo: List<BelvoIdentificationInfo>,
    )

    @Serializable
    private data class BelvoIdentificationInfo(
        val type: String,
        val number: String,
        val name: String,
    )

    @Serializable
    private data class BelvoWidgetTokenResponse(
        val access: String,
    )

    @Serializable
    private data class BelvoLinkResponse(
        val status: String,
    )
}
