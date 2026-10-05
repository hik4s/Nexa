package com.nexa.finance.application.openfinance

import com.nexa.finance.domain.common.DomainValidationException

class ConfiguredOpenFinanceProviderRegistry(
    private val providers: Map<String, OpenFinanceProvider> = emptyMap(),
) : OpenFinanceProviderRegistry {
    override fun get(providerId: String): OpenFinanceProvider =
        providers[providerId.trim().lowercase()]
            ?: throw DomainValidationException(
                "Open Finance provider '$providerId' is not configured for this environment.",
            )
}
