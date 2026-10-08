package com.nexa.finance.infrastructure.openfinance

import com.nexa.finance.domain.common.DomainValidationException
import java.net.URI

data class BelvoOpenFinanceSettings(
    val enabled: Boolean = false,
    val baseUrl: String = "https://sandbox.belvo.com",
    val secretId: String? = null,
    val secretPassword: String? = null,
    val termsAndConditionsUrl: String? = null,
    val successCallbackUrl: String? = null,
    val exitCallbackUrl: String? = null,
    val eventCallbackUrl: String? = null,
    val companyIconUrl: String? = null,
    val companyLogoUrl: String? = null,
    val companyName: String = "NEXA Finance",
    val purpose: String = "Organizar sua vida financeira com segurança.",
    val staleIn: String = "300d",
) {
    fun requireUsable() {
        if (!enabled) {
            throw DomainValidationException("Belvo Open Finance provider is disabled.")
        }
        requireSecret(secretId, "BELVO_SECRET_ID")
        requireSecret(secretPassword, "BELVO_SECRET_PASSWORD")
        requireHttpsUrl(baseUrl, "BELVO_BASE_URL")
        requireHttpsUrl(termsAndConditionsUrl, "BELVO_TERMS_URL")
        requireCallbackUrl(successCallbackUrl, "BELVO_CALLBACK_SUCCESS_URL")
        requireCallbackUrl(exitCallbackUrl, "BELVO_CALLBACK_EXIT_URL")
        requireCallbackUrl(eventCallbackUrl, "BELVO_CALLBACK_EVENT")
        requireHttpsUrl(companyIconUrl, "BELVO_COMPANY_ICON_URL")
        requireHttpsUrl(companyLogoUrl, "BELVO_COMPANY_LOGO_URL")
        if (companyName.isBlank() || companyName.length > 80) {
            throw DomainValidationException("BELVO_COMPANY_NAME must contain between 1 and 80 characters.")
        }
        if (purpose.isBlank() || purpose.length > 600) {
            throw DomainValidationException("BELVO_PURPOSE must contain between 1 and 600 characters.")
        }
        if (!Regex("""^\d{1,3}d$""").matches(staleIn)) {
            throw DomainValidationException("BELVO_STALE_IN must use the format <days>d.")
        }
    }

    companion object {
        fun fromEnvironment(environment: (String) -> String? = System::getenv): BelvoOpenFinanceSettings {
            val enabled = environment("BELVO_ENABLED").toBoolean()
            return BelvoOpenFinanceSettings(
                enabled = enabled,
                baseUrl = environment("BELVO_BASE_URL")?.trim()?.ifBlank { null } ?: "https://sandbox.belvo.com",
                secretId = environment("BELVO_SECRET_ID")?.trim()?.ifBlank { null },
                secretPassword = environment("BELVO_SECRET_PASSWORD")?.trim()?.ifBlank { null },
                termsAndConditionsUrl = environment("BELVO_TERMS_URL")?.trim()?.ifBlank { null },
                successCallbackUrl = environment("BELVO_CALLBACK_SUCCESS_URL")?.trim()?.ifBlank { null },
                exitCallbackUrl = environment("BELVO_CALLBACK_EXIT_URL")?.trim()?.ifBlank { null },
                eventCallbackUrl = environment("BELVO_CALLBACK_EVENT")?.trim()?.ifBlank { null },
                companyIconUrl = environment("BELVO_COMPANY_ICON_URL")?.trim()?.ifBlank { null },
                companyLogoUrl = environment("BELVO_COMPANY_LOGO_URL")?.trim()?.ifBlank { null },
                companyName = environment("BELVO_COMPANY_NAME")?.trim()?.ifBlank { null } ?: "NEXA Finance",
                purpose = environment("BELVO_PURPOSE")?.trim()?.ifBlank { null }
                    ?: "Organizar sua vida financeira com segurança.",
                staleIn = environment("BELVO_STALE_IN")?.trim()?.ifBlank { null } ?: "300d",
            )
        }

        private fun requireSecret(value: String?, name: String) {
            if (value.isNullOrBlank()) {
                throw DomainValidationException("$name is required when Belvo is enabled.")
            }
        }

        private fun requireCallbackUrl(value: String?, name: String) {
            val url = value?.takeIf { it.isNotBlank() }?.let {
                runCatching { URI(it) }.getOrNull()
            }
            val scheme = url?.scheme
            if (url == null || scheme.isNullOrBlank() ||
                (scheme != "https" && !Regex("""^[a-z][a-z0-9+.-]*$""").matches(scheme))
            ) {
                throw DomainValidationException("$name must be a valid HTTPS URL or application deep-link.")
            }
        }

        private fun requireHttpsUrl(value: String?, name: String) {
            val url = value?.takeIf { it.isNotBlank() }?.let {
                runCatching { URI(it) }.getOrNull()
            }
            if (url == null || url.scheme != "https" || url.host.isNullOrBlank()) {
                throw DomainValidationException("$name must be a valid HTTPS URL.")
            }
        }
    }
}
