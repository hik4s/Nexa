package com.nexa.finance.application.auth

import com.nexa.finance.domain.common.DomainValidationException
import com.nexa.finance.domain.common.UserId
import com.nexa.finance.infrastructure.user.JdbcUserAuthRepository
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

class ProfileService(
    private val repository: JdbcUserAuthRepository,
    private val clock: Clock = Clock.systemUTC(),
) {
    fun getProfile(userId: UserId): UserProfile =
        repository.findUser(userId)?.let {
            UserProfile(it.id, it.email, it.displayName, it.timeZone, it.status.name)
        } ?: throw DomainValidationException("User was not found.")

    fun updateProfile(
        userId: UserId,
        displayName: String? = null,
        email: String? = null,
        timeZone: ZoneId? = null,
    ): UserProfile {
        val user = repository.findUser(userId)
            ?: throw DomainValidationException("User was not found.")
        val now = Instant.now(clock)

        displayName?.let { user.changeDisplayName(it, now) }
        email?.let { user.changeEmail(it, now) }
        timeZone?.let { user.changeTimeZone(it, now) }

        repository.updateUser(user)
        return UserProfile(user.id, user.email, user.displayName, user.timeZone, user.status.name)
    }
}

data class UserProfile(
    val userId: UserId,
    val email: String,
    val displayName: String,
    val timeZone: ZoneId,
    val status: String,
)
