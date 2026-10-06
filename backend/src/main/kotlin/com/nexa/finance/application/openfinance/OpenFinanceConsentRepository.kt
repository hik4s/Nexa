package com.nexa.finance.application.openfinance

import com.nexa.finance.domain.common.UserId
import com.nexa.finance.domain.openfinance.OpenFinanceConsent
import com.nexa.finance.domain.openfinance.OpenFinanceConsentId
import java.time.Instant

interface OpenFinanceConsentRepository {
    fun create(consent: OpenFinanceConsent)
    fun findByOwner(ownerId: UserId): List<OpenFinanceConsent>
    fun findByOwnerAndId(ownerId: UserId, id: OpenFinanceConsentId): OpenFinanceConsent?
    fun update(consent: OpenFinanceConsent): OpenFinanceConsent?
    fun listUsable(ownerId: UserId, at: Instant): List<OpenFinanceConsent>
}
