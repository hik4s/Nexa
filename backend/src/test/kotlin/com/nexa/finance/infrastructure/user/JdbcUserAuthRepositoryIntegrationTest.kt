package com.nexa.finance.infrastructure.user

import com.nexa.finance.application.auth.AuthenticationService
import com.nexa.finance.application.auth.ProfileService
import com.nexa.finance.application.auth.SessionToken
import com.nexa.finance.domain.user.PasswordCredential
import com.nexa.finance.domain.user.RecoveryToken
import com.nexa.finance.domain.user.Session
import com.nexa.finance.domain.user.User
import com.nexa.finance.infrastructure.database.DatabaseConnectionProvider
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.junit.jupiter.api.Assumptions.assumeTrue

class JdbcUserAuthRepositoryIntegrationTest {
    @Test
    fun persistsIdentityAndSessionLifecycleAgainstPostgres() {
        assumeTrue(!System.getenv("POSTGRES_PASSWORD").isNullOrBlank())
        val clock = Clock.fixed(Instant.parse("2026-10-03T00:00:00Z"), ZoneOffset.UTC)
        val repository = JdbcUserAuthRepository(DatabaseConnectionProvider())
        val user = User.create(
            email = "integration-" + UUID.randomUUID() + "@example.com",
            displayName = "Integration User",
            clock = clock,
        )
        val credential = PasswordCredential.create("integration-value".toCharArray())

        repository.createUser(user, credential)

        try {
            val identity = repository.findByEmail(user.email)
            assertNotNull(identity)
            assertTrue(identity.credential.matches("integration-value".toCharArray()))

            val token = SessionToken.generate()
            val session = Session.create(
                userId = user.id,
                tokenHash = token.hash,
                createdAt = Instant.now(clock),
                expiresAt = Instant.now(clock).plusSeconds(3600),
            )
            repository.saveSession(session)

            val loaded = repository.findSession(token.hash)
            assertNotNull(loaded)
            assertTrue(loaded.isActive(Instant.parse("2026-10-03T00:30:00Z")))

            assertTrue(repository.revokeSession(token.hash, Instant.parse("2026-10-03T00:40:00Z")))
            assertFalse(repository.findSession(token.hash)!!.isActive(Instant.parse("2026-10-03T00:45:00Z")))
        } finally {
            deleteUser(user.id.value)
        }
    }

    @Test
    fun suspendedUserCannotContinueUsingExistingSessionAgainstPostgres() {
        assumeTrue(!System.getenv("POSTGRES_PASSWORD").isNullOrBlank())
        val clock = Clock.fixed(Instant.parse("2026-10-03T03:00:00Z"), ZoneOffset.UTC)
        val repository = JdbcUserAuthRepository(DatabaseConnectionProvider())
        val user = User.create(
            email = "authorization-" + UUID.randomUUID() + "@example.com",
            displayName = "Authorization User",
            clock = clock,
        )
        repository.createUser(user, PasswordCredential.create("auth-password".toCharArray()))

        try {
            val authentication = AuthenticationService(repository, clock)
            val session = authentication.login(user.email, "auth-password".toCharArray())
            assertTrue(authentication.authenticate(session.token, Instant.parse("2026-10-03T03:01:00Z")) == user.id)

            user.suspend(Instant.parse("2026-10-03T03:02:00Z"))
            repository.updateUser(user)

            assertFailsWith<com.nexa.finance.domain.common.DomainValidationException> {
                authentication.authenticate(session.token, Instant.parse("2026-10-03T03:03:00Z"))
            }
        } finally {
            deleteUser(user.id.value)
        }
    }

    @Test
    fun updatesBasicProfileAgainstPostgres() {
        assumeTrue(!System.getenv("POSTGRES_PASSWORD").isNullOrBlank())
        val clock = Clock.fixed(Instant.parse("2026-10-03T02:00:00Z"), ZoneOffset.UTC)
        val repository = JdbcUserAuthRepository(DatabaseConnectionProvider())
        val user = User.create(
            email = "profile-" + UUID.randomUUID() + "@example.com",
            displayName = "Original Name",
            timeZone = ZoneOffset.UTC,
            clock = clock,
        )
        repository.createUser(user, PasswordCredential.create("profile-password".toCharArray()))

        try {
            val service = ProfileService(repository, clock)
            val before = service.getProfile(user.id)
            assertTrue(before.displayName == "Original Name")
            assertTrue(before.email == user.email)

            val after = service.updateProfile(
                userId = user.id,
                displayName = "Updated Name",
                timeZone = java.time.ZoneId.of("America/Sao_Paulo"),
            )

            assertTrue(after.displayName == "Updated Name")
            assertTrue(after.timeZone == java.time.ZoneId.of("America/Sao_Paulo"))
            assertTrue(service.getProfile(user.id).displayName == "Updated Name")
        } finally {
            deleteUser(user.id.value)
        }
    }

    @Test
    fun resetsPasswordConsumesRecoveryTokenAndRevokesSessionsAgainstPostgres() {
        assumeTrue(!System.getenv("POSTGRES_PASSWORD").isNullOrBlank())
        val clock = Clock.fixed(Instant.parse("2026-10-03T01:00:00Z"), ZoneOffset.UTC)
        val repository = JdbcUserAuthRepository(DatabaseConnectionProvider())
        val user = User.create(
            email = "recovery-" + UUID.randomUUID() + "@example.com",
            displayName = "Recovery User",
            clock = clock,
        )
        repository.createUser(user, PasswordCredential.create("old-password".toCharArray()))

        try {
            val sessionToken = SessionToken.generate()
            repository.saveSession(
                Session.create(
                    userId = user.id,
                    tokenHash = sessionToken.hash,
                    createdAt = Instant.now(clock),
                    expiresAt = Instant.now(clock).plusSeconds(3600),
                )
            )

            val recoveryToken = SessionToken.generate()
            repository.saveRecoveryToken(
                RecoveryToken.create(
                    userId = user.id,
                    tokenHash = recoveryToken.hash,
                    createdAt = Instant.now(clock),
                    expiresAt = Instant.now(clock).plusSeconds(1800),
                )
            )

            val resetAt = Instant.parse("2026-10-03T01:10:00Z")
            assertTrue(repository.resetPassword(
                recoveryToken.hash,
                PasswordCredential.create("new-password".toCharArray()),
                resetAt,
            ) == user.id)

            assertTrue(repository.findByEmail(user.email)!!.credential.matches("new-password".toCharArray()))
            assertFalse(repository.findSession(sessionToken.hash)!!.isActive(resetAt.plusSeconds(1)))
            assertFalse(repository.findRecoveryToken(recoveryToken.hash)!!.isUsable(resetAt.plusSeconds(1)))
        } finally {
            deleteUser(user.id.value)
        }
    }

    private fun deleteUser(userId: UUID) {
        DatabaseConnectionProvider().open().use { connection ->
            connection.prepareStatement("DELETE FROM users WHERE id = ?").use { statement ->
                statement.setObject(1, userId)
                statement.executeUpdate()
            }
        }
    }
}
