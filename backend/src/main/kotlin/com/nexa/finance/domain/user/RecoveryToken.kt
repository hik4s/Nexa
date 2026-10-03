package com.nexa.finance.domain.user

import com.nexa.finance.domain.common.DomainValidationException
import com.nexa.finance.domain.common.UserId
import java.time.Instant
import java.util.UUID

class RecoveryToken private constructor(
    val id: UUID,
    val userId: UserId,
    val tokenHash: String,
    val createdAt: Instant,
    val expiresAt: Instant,
    usedAt: Instant?,
) {
    var usedAt: Instant? = usedAt
        private set

    fun isUsable(now: Instant): Boolean = usedAt == null && now.isBefore(expiresAt)

    fun consume(now: Instant) {
        if (!isUsable(now)) throw DomainValidationException("Recovery token is no longer usable.")
        usedAt = now
    }

    companion object {
        fun create(
            userId: UserId,
            tokenHash: String,
            createdAt: Instant,
            expiresAt: Instant,
            id: UUID = UUID.randomUUID(),
        ): RecoveryToken {
            if (tokenHash.isBlank()) throw DomainValidationException("Recovery token hash cannot be blank.")
            if (!expiresAt.isAfter(createdAt)) throw DomainValidationException("Recovery token expiry must be after creation.")
            return RecoveryToken(id, userId, tokenHash, createdAt, expiresAt, null)
        }
    }
}
