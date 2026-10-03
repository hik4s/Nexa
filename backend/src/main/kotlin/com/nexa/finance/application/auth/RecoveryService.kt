package com.nexa.finance.application.auth

import com.nexa.finance.domain.common.DomainValidationException
import com.nexa.finance.domain.common.UserId
import com.nexa.finance.domain.user.PasswordCredential
import com.nexa.finance.domain.user.RecoveryToken
import com.nexa.finance.domain.user.UserStatus
import com.nexa.finance.infrastructure.user.JdbcUserAuthRepository
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.Locale

class RecoveryService(
    private val repository: JdbcUserAuthRepository,
    private val clock: Clock = Clock.systemUTC(),
    private val tokenLifetime: Duration = Duration.ofMinutes(30),
) {
    fun requestRecovery(email: String): RecoveryRequest {
        val normalizedEmail = email.trim().lowercase(Locale.ROOT)
        val identity = repository.findByEmail(normalizedEmail)
            ?: throw DomainValidationException("Invalid recovery request.")
        if (identity.status != UserStatus.ACTIVE) {
            throw DomainValidationException("Invalid recovery request.")
        }

        val generated = SessionToken.generate()
        val now = Instant.now(clock)
        val token = RecoveryToken.create(
            userId = identity.userId,
            tokenHash = generated.hash,
            createdAt = now,
            expiresAt = now.plus(tokenLifetime),
        )
        repository.saveRecoveryToken(token)
        return RecoveryRequest(identity.userId, generated.raw, token.expiresAt)
    }

    fun resetPassword(token: String, newPassword: CharArray, now: Instant = Instant.now(clock)): UserId {
        if (token.isBlank()) throw DomainValidationException("Invalid recovery token.")
        val credential = PasswordCredential.create(newPassword)
        return repository.resetPassword(SessionToken.hash(token), credential, now)
    }
}

data class RecoveryRequest(
    val userId: UserId,
    val token: String,
    val expiresAt: Instant,
)
