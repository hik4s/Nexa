package com.nexa.finance.application.auth

import com.nexa.finance.application.audit.AuditEventRecorder
import com.nexa.finance.domain.common.DomainValidationException
import com.nexa.finance.domain.common.UserId
import com.nexa.finance.domain.user.PasswordCredential
import com.nexa.finance.domain.user.Session
import com.nexa.finance.domain.user.User
import com.nexa.finance.domain.user.UserStatus
import com.nexa.finance.infrastructure.user.JdbcUserAuthRepository
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.util.Locale

class AuthenticationService(
    private val repository: JdbcUserAuthRepository,
    private val clock: Clock = Clock.systemUTC(),
    private val sessionLifetime: Duration = Duration.ofDays(30),
    private val audit: AuditEventRecorder? = null,
) {
    fun register(email: String, displayName: String, timeZone: ZoneId = ZoneId.of("UTC"), password: CharArray): User {
        val user = User.create(email, displayName, timeZone, clock = clock)
        repository.createUser(user, PasswordCredential.create(password))
        audit?.record(
            actorUserId = user.id,
            eventType = "AUTH_REGISTERED",
            targetType = "USER",
            targetId = user.id.value,
        )
        return user
    }

    fun login(email: String, password: CharArray): AuthenticatedSession {
        val identity = repository.findByEmail(email.trim().lowercase(Locale.ROOT))
            ?: run {
                audit?.record(actorUserId = null, eventType = "AUTH_LOGIN_FAILED", metadata = "{\"reason\":\"invalid_credentials\"}")
                throw DomainValidationException("Invalid credentials.")
            }
        if (identity.status != UserStatus.ACTIVE || !identity.credential.matches(password)) {
            audit?.record(
                actorUserId = identity.userId,
                eventType = "AUTH_LOGIN_FAILED",
                targetType = "USER",
                targetId = identity.userId.value,
                metadata = "{\"reason\":\"invalid_credentials\"}",
            )
            throw DomainValidationException("Invalid credentials.")
        }
        val now = Instant.now(clock)
        val token = SessionToken.generate()
        val session = Session.create(identity.userId, token.hash, now, now.plus(sessionLifetime))
        repository.saveSession(session)
        audit?.record(
            actorUserId = identity.userId,
            eventType = "AUTH_LOGIN_SUCCEEDED",
            targetType = "SESSION",
            targetId = session.id,
        )
        return AuthenticatedSession(identity.userId, token.raw, session.expiresAt)
    }

    fun logout(token: String, now: Instant = Instant.now(clock)): Boolean {
        val revoked = repository.revokeSession(SessionToken.hash(token), now)
        if (revoked) {
            audit?.record(actorUserId = null, eventType = "AUTH_LOGOUT")
        }
        return revoked
    }

    fun authenticate(token: String, now: Instant = Instant.now(clock)): UserId {
        val session = repository.findSession(SessionToken.hash(token))
            ?: throw DomainValidationException("Invalid session.")
        if (!session.isActive(now)) throw DomainValidationException("Invalid session.")
        val user = repository.findUser(session.userId)
            ?: throw DomainValidationException("Invalid session.")
        if (user.status != UserStatus.ACTIVE) throw DomainValidationException("Invalid session.")
        return session.userId
    }
}

data class AuthenticatedSession(val userId: UserId, val token: String, val expiresAt: Instant)
