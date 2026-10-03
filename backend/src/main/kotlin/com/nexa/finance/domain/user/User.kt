package com.nexa.finance.domain.user

import com.nexa.finance.domain.common.DomainValidationException
import com.nexa.finance.domain.common.UserId
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.util.Locale

enum class UserStatus {
    ACTIVE,
    SUSPENDED,
    DEACTIVATED,
}

class User private constructor(
    val id: UserId,
    email: String,
    displayName: String,
    timeZone: ZoneId,
    val createdAt: Instant,
    updatedAt: Instant,
    status: UserStatus,
) {
    var email: String = normalizeEmail(email)
        private set

    var displayName: String = validateDisplayName(displayName)
        private set

    var timeZone: ZoneId = timeZone
        private set

    var updatedAt: Instant = updatedAt
        private set

    var status: UserStatus = status
        private set

    fun changeEmail(newEmail: String, now: Instant) {
        ensureNotDeactivated()
        email = normalizeEmail(newEmail)
        updatedAt = now
    }

    fun changeDisplayName(newDisplayName: String, now: Instant) {
        ensureNotDeactivated()
        displayName = validateDisplayName(newDisplayName)
        updatedAt = now
    }

    fun changeTimeZone(newTimeZone: ZoneId, now: Instant) {
        ensureNotDeactivated()
        timeZone = newTimeZone
        updatedAt = now
    }

    fun suspend(now: Instant) {
        if (status == UserStatus.DEACTIVATED) {
            throw DomainValidationException("Deactivated users cannot be suspended.")
        }
        status = UserStatus.SUSPENDED
        updatedAt = now
    }

    fun reactivate(now: Instant) {
        if (status == UserStatus.DEACTIVATED) {
            throw DomainValidationException("Deactivated users cannot be reactivated.")
        }
        status = UserStatus.ACTIVE
        updatedAt = now
    }

    fun deactivate(now: Instant) {
        if (status == UserStatus.DEACTIVATED) return
        status = UserStatus.DEACTIVATED
        updatedAt = now
    }

    fun ensureActive() {
        if (status != UserStatus.ACTIVE) {
            throw DomainValidationException("User must be active for this operation.")
        }
    }

    private fun ensureNotDeactivated() {
        if (status == UserStatus.DEACTIVATED) {
            throw DomainValidationException("Deactivated users cannot be modified.")
        }
    }

    companion object {
        private val EMAIL_REGEX = Regex("""^[^\s@]+@[^\s@]+\.[^\s@]+$""")

        fun create(
            email: String,
            displayName: String,
            timeZone: ZoneId = ZoneId.of("UTC"),
            clock: Clock = Clock.systemUTC(),
            id: UserId = UserId.new(),
        ): User {
            val now = Instant.now(clock)
            return User(
                id = id,
                email = email,
                displayName = displayName,
                timeZone = timeZone,
                createdAt = now,
                updatedAt = now,
                status = UserStatus.ACTIVE,
            )
        }

        private fun normalizeEmail(value: String): String {
            val normalized = value.trim().lowercase(Locale.ROOT)
            if (normalized.length > 254 || !EMAIL_REGEX.matches(normalized)) {
                throw DomainValidationException("User email is invalid.")
            }
            return normalized
        }

        private fun validateDisplayName(value: String): String {
            val normalized = value.trim()
            if (normalized.length !in 1..80) {
                throw DomainValidationException("Display name must contain between 1 and 80 characters.")
            }
            return normalized
        }
    }
}
