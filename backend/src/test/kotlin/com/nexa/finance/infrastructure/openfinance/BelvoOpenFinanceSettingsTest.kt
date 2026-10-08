package com.nexa.finance.infrastructure.openfinance

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BelvoOpenFinanceSettingsTest {
    @Test
    fun disabledByDefault() {
        val settings = BelvoOpenFinanceSettings.fromEnvironment { null }

        assertFalse(settings.enabled)
        assertFailsWith<com.nexa.finance.domain.common.DomainValidationException> {
            settings.requireUsable()
        }
    }

    @Test
    fun enabledConfigurationRequiresCredentialsAndSecureEndpoints() {
        val environment = mapOf(
            "BELVO_ENABLED" to "true",
            "BELVO_BASE_URL" to "https://sandbox.belvo.com",
            "BELVO_SECRET_ID" to "secret-id",
            "BELVO_SECRET_PASSWORD" to "secret-password",
            "BELVO_TERMS_URL" to "https://nexa.example/legal/terms",
            "BELVO_CALLBACK_SUCCESS_URL" to "nexa://open-finance/success",
            "BELVO_CALLBACK_EXIT_URL" to "nexa://open-finance/exit",
            "BELVO_CALLBACK_EVENT" to "nexa://open-finance/error",
            "BELVO_COMPANY_ICON_URL" to "https://nexa.example/icon.svg",
            "BELVO_COMPANY_LOGO_URL" to "https://nexa.example/logo.svg",
            "BELVO_COMPANY_NAME" to "NEXA Finance",
            "BELVO_STALE_IN" to "300d",
        )
        val settings = BelvoOpenFinanceSettings.fromEnvironment(environment::get)

        assertTrue(settings.enabled)
        settings.requireUsable()
        assertEquals("https://sandbox.belvo.com", settings.baseUrl)
        assertEquals("300d", settings.staleIn)
    }

    @Test
    fun enabledConfigurationRejectsInsecureTermsUrl() {
        val environment = mapOf(
            "BELVO_ENABLED" to "true",
            "BELVO_BASE_URL" to "https://sandbox.belvo.com",
            "BELVO_SECRET_ID" to "secret-id",
            "BELVO_SECRET_PASSWORD" to "secret-password",
            "BELVO_TERMS_URL" to "http://nexa.example/legal/terms",
            "BELVO_CALLBACK_SUCCESS_URL" to "nexa://open-finance/success",
            "BELVO_CALLBACK_EXIT_URL" to "nexa://open-finance/exit",
            "BELVO_CALLBACK_EVENT" to "nexa://open-finance/error",
            "BELVO_COMPANY_ICON_URL" to "https://nexa.example/icon.svg",
            "BELVO_COMPANY_LOGO_URL" to "https://nexa.example/logo.svg",
        )
        val settings = BelvoOpenFinanceSettings.fromEnvironment(environment::get)

        assertFailsWith<com.nexa.finance.domain.common.DomainValidationException> {
            settings.requireUsable()
        }
    }

    @Test
    fun callbackMayUseHttpsOrApplicationDeepLink() {
        val common = mapOf(
            "BELVO_ENABLED" to "true",
            "BELVO_BASE_URL" to "https://sandbox.belvo.com",
            "BELVO_SECRET_ID" to "secret-id",
            "BELVO_SECRET_PASSWORD" to "secret-password",
            "BELVO_TERMS_URL" to "https://nexa.example/legal/terms",
            "BELVO_CALLBACK_SUCCESS_URL" to "https://nexa.example/open-finance/success",
            "BELVO_CALLBACK_EXIT_URL" to "nexa://open-finance/exit",
            "BELVO_CALLBACK_EVENT" to "nexa://open-finance/error",
            "BELVO_COMPANY_ICON_URL" to "https://nexa.example/icon.svg",
            "BELVO_COMPANY_LOGO_URL" to "https://nexa.example/logo.svg",
        )

        BelvoOpenFinanceSettings.fromEnvironment(common::get).requireUsable()
        assertTrue(true)
    }
}
