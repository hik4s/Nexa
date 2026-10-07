package com.nexa.finance.domain.user

import com.nexa.finance.domain.common.DomainValidationException
import com.nexa.finance.domain.common.UserId
import java.time.Instant
import java.util.UUID

class Session private constructor(
    val id: UUID,
    val userId: UserId,
    val tokenHash: String,
    val createdAt: Instant,
    val expiresAt: Instant,
    revokedAt: Instant?,
    lastSeenAt: Instant,
) {
    var revokedAt: Instant? = revokedAt
        private set
    var lastSeenAt: Instant = lastSeenAt
        private set

    fun isActive(now: Instant): Boolean = revokedAt == null && now.isBefore(expiresAt)

    fun touch(now: Instant) {
        if (!isActive(now)) throw DomainValidationException("Session is not active.")
        lastSeenAt = now
    }

    fun revoke(now: Instant) {
        if (revokedAt == null) revokedAt = now
    }

    companion object {
        fun create(
            userId: UserId,
            tokenHash: String,
            createdAt: Instant,
            expiresAt: Instant,
            id: UUID = UUID.randomUUID(),
        ): Session {
            if (tokenHash.isBlank()) throw DomainValidationException("Session token hash cannot be blank.")
            if (!expiresAt.isAfter(createdAt)) throw DomainValidationException("Session expiry must be after creation.")
            return Session(id, userId, tokenHash, createdAt, expiresAt, null, createdAt)
        }
    }
}
