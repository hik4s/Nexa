package com.nexa.finance.infrastructure.openfinance

import com.nexa.finance.domain.common.UserId
import com.nexa.finance.domain.openfinance.OpenFinanceConsent
import com.nexa.finance.domain.openfinance.OpenFinanceConsentId
import com.nexa.finance.domain.openfinance.OpenFinanceConsentStatus
import com.nexa.finance.infrastructure.database.DatabaseConnectionProvider
import com.nexa.finance.application.openfinance.OpenFinanceConsentRepository
import java.sql.Timestamp
import java.time.Instant
import java.util.UUID

class JdbcOpenFinanceConsentRepository(
    private val connections: DatabaseConnectionProvider = DatabaseConnectionProvider(),
) : OpenFinanceConsentRepository {
    override fun create(consent: OpenFinanceConsent) {
        connections.open().use { c ->
            c.prepareStatement(
                """
                INSERT INTO open_finance_consents
                    (id, user_id, provider_id, external_consent_id, scopes_json, status,
                     created_at, updated_at, expires_at, revoked_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent(),
            ).use { s ->
                s.setObject(1, consent.id)
                s.setObject(2, consent.userId.value)
                s.setString(3, consent.providerId)
                s.setString(4, consent.externalConsentId)
                s.setString(5, encodeScopes(consent.scopes))
                s.setString(6, consent.status.name)
                s.setTimestamp(7, Timestamp.from(consent.createdAt))
                s.setTimestamp(8, Timestamp.from(consent.updatedAt))
                if (consent.expiresAt == null) s.setNull(9, java.sql.Types.TIMESTAMP_WITH_TIMEZONE)
                else s.setTimestamp(9, Timestamp.from(consent.expiresAt))
                if (consent.revokedAt == null) s.setNull(10, java.sql.Types.TIMESTAMP_WITH_TIMEZONE)
                else s.setTimestamp(10, Timestamp.from(consent.revokedAt))
                s.executeUpdate()
            }
        }
    }

    override fun findByOwner(ownerId: UserId): List<OpenFinanceConsent> =
        connections.open().use { c ->
            c.prepareStatement(
                """
                SELECT id, user_id, provider_id, external_consent_id, scopes_json, status,
                       created_at, updated_at, expires_at, revoked_at
                FROM open_finance_consents
                WHERE user_id = ?
                ORDER BY updated_at DESC, id DESC
                """.trimIndent(),
            ).use { s ->
                s.setObject(1, ownerId.value)
                s.executeQuery().use { r ->
                    buildList {
                        while (r.next()) add(r.toConsent())
                    }
                }
            }
        }

    override fun findByOwnerAndId(ownerId: UserId, id: OpenFinanceConsentId): OpenFinanceConsent? =
        connections.open().use { c ->
            c.prepareStatement(
                """
                SELECT id, user_id, provider_id, external_consent_id, scopes_json, status,
                       created_at, updated_at, expires_at, revoked_at
                FROM open_finance_consents
                WHERE user_id = ? AND id = ?
                """.trimIndent(),
            ).use { s ->
                s.setObject(1, ownerId.value)
                s.setObject(2, id)
                s.executeQuery().use { r ->
                    if (r.next()) r.toConsent() else null
                }
            }
        }

    override fun update(consent: OpenFinanceConsent): OpenFinanceConsent? =
        connections.open().use { c ->
            c.prepareStatement(
                """
                UPDATE open_finance_consents
                SET external_consent_id = ?, scopes_json = ?, status = ?,
                    updated_at = ?, expires_at = ?, revoked_at = ?
                WHERE user_id = ? AND id = ?
                RETURNING id, user_id, provider_id, external_consent_id, scopes_json, status,
                          created_at, updated_at, expires_at, revoked_at
                """.trimIndent(),
            ).use { s ->
                s.setString(1, consent.externalConsentId)
                s.setString(2, encodeScopes(consent.scopes))
                s.setString(3, consent.status.name)
                s.setTimestamp(4, Timestamp.from(consent.updatedAt))
                if (consent.expiresAt == null) s.setNull(5, java.sql.Types.TIMESTAMP_WITH_TIMEZONE)
                else s.setTimestamp(5, Timestamp.from(consent.expiresAt))
                if (consent.revokedAt == null) s.setNull(6, java.sql.Types.TIMESTAMP_WITH_TIMEZONE)
                else s.setTimestamp(6, Timestamp.from(consent.revokedAt))
                s.setObject(7, consent.userId.value)
                s.setObject(8, consent.id)
                s.executeQuery().use { r ->
                    if (r.next()) r.toConsent() else null
                }
            }
        }

    override fun listUsable(ownerId: UserId, at: Instant): List<OpenFinanceConsent> =
        connections.open().use { c ->
            c.prepareStatement(
                """
                SELECT id, user_id, provider_id, external_consent_id, scopes_json, status,
                       created_at, updated_at, expires_at, revoked_at
                FROM open_finance_consents
                WHERE user_id = ?
                  AND status = 'ACTIVE'
                  AND (expires_at IS NULL OR expires_at > ?)
                ORDER BY updated_at DESC, id DESC
                """.trimIndent(),
            ).use { s ->
                s.setObject(1, ownerId.value)
                s.setTimestamp(2, Timestamp.from(at))
                s.executeQuery().use { r ->
                    buildList {
                        while (r.next()) add(r.toConsent())
                    }
                }
            }
        }

    private fun encodeScopes(scopes: Set<String>): String =
        scopes.toList().sorted().joinToString(",")

    private fun decodeScopes(value: String): Set<String> =
        value.split(',')
            .map(String::trim)
            .filter(String::isNotBlank)
            .toSet()

    private fun java.sql.ResultSet.toConsent(): OpenFinanceConsent =
        OpenFinanceConsent.restore(
            id = getObject("id", UUID::class.java),
            userId = UserId(getObject("user_id", UUID::class.java)),
            providerId = getString("provider_id"),
            externalConsentId = getString("external_consent_id"),
            scopes = decodeScopes(getString("scopes_json")),
            status = OpenFinanceConsentStatus.valueOf(getString("status")),
            createdAt = getTimestamp("created_at").toInstant(),
            updatedAt = getTimestamp("updated_at").toInstant(),
            expiresAt = getTimestamp("expires_at")?.toInstant(),
            revokedAt = getTimestamp("revoked_at")?.toInstant(),
        )
}
