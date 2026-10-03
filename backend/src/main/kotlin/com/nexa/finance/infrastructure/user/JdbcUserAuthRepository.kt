package com.nexa.finance.infrastructure.user

import com.nexa.finance.domain.common.DomainValidationException
import com.nexa.finance.domain.common.UserId
import com.nexa.finance.domain.user.PasswordCredential
import com.nexa.finance.domain.user.RecoveryToken
import com.nexa.finance.domain.user.Session
import com.nexa.finance.domain.user.User
import com.nexa.finance.domain.user.UserStatus
import com.nexa.finance.infrastructure.database.DatabaseConnectionProvider
import java.sql.ResultSet
import java.sql.Timestamp
import java.time.Instant
import java.util.UUID

data class UserIdentity(val userId: UserId, val status: UserStatus, val credential: PasswordCredential)

class JdbcUserAuthRepository(
    private val connections: DatabaseConnectionProvider = DatabaseConnectionProvider(),
) {
    fun createUser(user: User, credential: PasswordCredential) {
        try {
            connections.open().use { connection ->
                connection.autoCommit = false
                connection.prepareStatement(
                    "INSERT INTO users (id, email, display_name, time_zone, status, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?)"
                ).use { s ->
                    s.setObject(1, user.id.value); s.setString(2, user.email); s.setString(3, user.displayName)
                    s.setString(4, user.timeZone.id); s.setString(5, user.status.name)
                    s.setTimestamp(6, Timestamp.from(user.createdAt)); s.setTimestamp(7, Timestamp.from(user.updatedAt)); s.executeUpdate()
                }
                connection.prepareStatement(
                    "INSERT INTO user_credentials (user_id, password_hash, password_algorithm, created_at, updated_at) VALUES (?, ?, ?, ?, ?)"
                ).use { s ->
                    s.setObject(1, user.id.value); s.setString(2, credential.encodedHash); s.setString(3, credential.algorithm)
                    s.setTimestamp(4, Timestamp.from(user.createdAt)); s.setTimestamp(5, Timestamp.from(user.updatedAt)); s.executeUpdate()
                }
                connection.commit()
            }
        } catch (e: java.sql.SQLException) {
            if (e.sqlState == "23505") throw DomainValidationException("User email is already registered.")
            throw e
        }
    }

    fun findByEmail(email: String): UserIdentity? =
        connections.open().use { c ->
            c.prepareStatement(
                "SELECT u.id, u.status, c.password_hash, c.password_algorithm FROM users u JOIN user_credentials c ON c.user_id = u.id WHERE u.email = ?"
            ).use { s ->
                s.setString(1, email)
                s.executeQuery().use { r -> if (r.next()) r.toIdentity() else null }
            }
        }

    fun saveSession(session: Session) {
        connections.open().use { c ->
            c.prepareStatement(
                "INSERT INTO user_sessions (id, user_id, token_hash, created_at, expires_at, revoked_at, last_seen_at) VALUES (?, ?, ?, ?, ?, ?, ?)"
            ).use { s ->
                s.setObject(1, session.id); s.setObject(2, session.userId.value); s.setString(3, session.tokenHash)
                s.setTimestamp(4, Timestamp.from(session.createdAt)); s.setTimestamp(5, Timestamp.from(session.expiresAt))
                if (session.revokedAt == null) s.setTimestamp(6, null) else s.setTimestamp(6, Timestamp.from(session.revokedAt))
                s.setTimestamp(7, Timestamp.from(session.lastSeenAt)); s.executeUpdate()
            }
        }
    }

    fun revokeSession(tokenHash: String, now: Instant): Boolean =
        connections.open().use { c ->
            c.prepareStatement("UPDATE user_sessions SET revoked_at = ? WHERE token_hash = ? AND revoked_at IS NULL").use { s ->
                s.setTimestamp(1, Timestamp.from(now)); s.setString(2, tokenHash); s.executeUpdate() > 0
            }
        }

    fun findSession(tokenHash: String): Session? =
        connections.open().use { c ->
            c.prepareStatement(
                "SELECT id, user_id, token_hash, created_at, expires_at, revoked_at, last_seen_at FROM user_sessions WHERE token_hash = ?"
            ).use { s ->
                s.setString(1, tokenHash)
                s.executeQuery().use { r ->
                    if (!r.next()) return@use null
                    Session.create(
                        userId = UserId(r.getObject("user_id", UUID::class.java)),
                        tokenHash = r.getString("token_hash"),
                        createdAt = r.getTimestamp("created_at").toInstant(),
                        expiresAt = r.getTimestamp("expires_at").toInstant(),
                        id = r.getObject("id", UUID::class.java),
                    ).also { r.getTimestamp("revoked_at")?.toInstant()?.let(it::revoke) }
                }
            }
        }

    fun saveRecoveryToken(token: RecoveryToken) {
        connections.open().use { c ->
            c.autoCommit = false
            try {
                c.prepareStatement(
                    "DELETE FROM password_recovery_tokens WHERE user_id = ? AND used_at IS NULL"
                ).use { s ->
                    s.setObject(1, token.userId.value)
                    s.executeUpdate()
                }
                c.prepareStatement(
                    "INSERT INTO password_recovery_tokens (id, user_id, token_hash, created_at, expires_at, used_at) VALUES (?, ?, ?, ?, ?, NULL)"
                ).use { s ->
                    s.setObject(1, token.id)
                    s.setObject(2, token.userId.value)
                    s.setString(3, token.tokenHash)
                    s.setTimestamp(4, Timestamp.from(token.createdAt))
                    s.setTimestamp(5, Timestamp.from(token.expiresAt))
                    s.executeUpdate()
                }
                c.commit()
            } catch (e: Exception) {
                c.rollback()
                throw e
            }
        }
    }

    fun findRecoveryToken(tokenHash: String): RecoveryToken? =
        connections.open().use { c ->
            c.prepareStatement(
                "SELECT id, user_id, token_hash, created_at, expires_at, used_at FROM password_recovery_tokens WHERE token_hash = ?"
            ).use { s ->
                s.setString(1, tokenHash)
                s.executeQuery().use { r ->
                    if (!r.next()) return@use null
                    RecoveryToken.create(
                        id = r.getObject("id", UUID::class.java),
                        userId = UserId(r.getObject("user_id", UUID::class.java)),
                        tokenHash = r.getString("token_hash"),
                        createdAt = r.getTimestamp("created_at").toInstant(),
                        expiresAt = r.getTimestamp("expires_at").toInstant(),
                    ).also { loaded ->
                        r.getTimestamp("used_at")?.toInstant()?.let(loaded::consume)
                    }
                }
            }
        }

    fun resetPassword(tokenHash: String, newCredential: PasswordCredential, now: Instant): UserId {
        connections.open().use { c ->
            c.autoCommit = false
            try {
                val recovery = c.prepareStatement(
                    "SELECT id, user_id, token_hash, created_at, expires_at, used_at FROM password_recovery_tokens WHERE token_hash = ? FOR UPDATE"
                ).use { s ->
                    s.setString(1, tokenHash)
                    s.executeQuery().use { r ->
                        if (!r.next()) throw DomainValidationException("Invalid recovery token.")
                        RecoveryToken.create(
                            id = r.getObject("id", UUID::class.java),
                            userId = UserId(r.getObject("user_id", UUID::class.java)),
                            tokenHash = r.getString("token_hash"),
                            createdAt = r.getTimestamp("created_at").toInstant(),
                            expiresAt = r.getTimestamp("expires_at").toInstant(),
                        ).also { loaded ->
                            val usedAt = r.getTimestamp("used_at")?.toInstant()
                            if (usedAt != null) loaded.consume(usedAt)
                            if (!loaded.isUsable(now)) throw DomainValidationException("Invalid recovery token.")
                        }
                    }
                }

                c.prepareStatement(
                    "UPDATE user_credentials SET password_hash = ?, password_algorithm = ?, updated_at = ? WHERE user_id = ?"
                ).use { s ->
                    s.setString(1, newCredential.encodedHash)
                    s.setString(2, newCredential.algorithm)
                    s.setTimestamp(3, Timestamp.from(now))
                    s.setObject(4, recovery.userId.value)
                    if (s.executeUpdate() != 1) throw DomainValidationException("User credential was not found.")
                }

                c.prepareStatement("UPDATE password_recovery_tokens SET used_at = ? WHERE id = ?").use { s ->
                    s.setTimestamp(1, Timestamp.from(now))
                    s.setObject(2, recovery.id)
                    s.executeUpdate()
                }

                c.prepareStatement(
                    "UPDATE user_sessions SET revoked_at = ? WHERE user_id = ? AND revoked_at IS NULL"
                ).use { s ->
                    s.setTimestamp(1, Timestamp.from(now))
                    s.setObject(2, recovery.userId.value)
                    s.executeUpdate()
                }

                c.commit()
                return recovery.userId
            } catch (e: Exception) {
                c.rollback()
                throw e
            }
        }
    }

    fun findUser(userId: UserId): User? =
        connections.open().use { c ->
            c.prepareStatement(
                "SELECT id, email, display_name, time_zone, status, created_at, updated_at FROM users WHERE id = ?"
            ).use { s ->
                s.setObject(1, userId.value)
                s.executeQuery().use { r ->
                    if (!r.next()) return@use null
                    User.restore(
                        id = UserId(r.getObject("id", UUID::class.java)),
                        email = r.getString("email"),
                        displayName = r.getString("display_name"),
                        timeZone = java.time.ZoneId.of(r.getString("time_zone")),
                        createdAt = r.getTimestamp("created_at").toInstant(),
                        updatedAt = r.getTimestamp("updated_at").toInstant(),
                        status = UserStatus.valueOf(r.getString("status")),
                    )
                }
            }
        }

    fun updateUser(user: User) {
        try {
            connections.open().use { c ->
                c.prepareStatement(
                    "UPDATE users SET email = ?, display_name = ?, time_zone = ?, status = ?, updated_at = ? WHERE id = ?"
                ).use { s ->
                    s.setString(1, user.email)
                    s.setString(2, user.displayName)
                    s.setString(3, user.timeZone.id)
                    s.setString(4, user.status.name)
                    s.setTimestamp(5, Timestamp.from(user.updatedAt))
                    s.setObject(6, user.id.value)
                    if (s.executeUpdate() != 1) throw DomainValidationException("User was not found.")
                }
            }
        } catch (e: java.sql.SQLException) {
            if (e.sqlState == "23505") throw DomainValidationException("User email is already registered.")
            throw e
        }
    }

    private fun ResultSet.toIdentity() = UserIdentity(
        UserId(getObject("id", UUID::class.java)),
        UserStatus.valueOf(getString("status")),
        PasswordCredential.restore(getString("password_hash"), getString("password_algorithm")),
    )
}
