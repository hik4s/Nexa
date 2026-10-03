package com.nexa.finance.domain.user

import com.nexa.finance.domain.common.DomainValidationException
import com.nexa.finance.domain.common.UserId
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith

class CredentialSecurityTest {
    @Test
    fun generatedCredentialMatchesOnlyOriginalValue() {
        val value = "nexa-test-value".toCharArray()
        val credential = PasswordCredential.create(value)

        assertFalse(credential.encodedHash.contains("nexa-test-value"))
        assertTrue(credential.matches("nexa-test-value".toCharArray()))
        assertFalse(credential.matches("different-value".toCharArray()))
    }

    @Test
    fun weakValueIsRejected() {
        assertFailsWith<DomainValidationException> {
            PasswordCredential.create("short".toCharArray())
        }
    }

    @Test
    fun sessionExpiresAndCanBeRevoked() {
        val start = Instant.parse("2026-10-03T00:00:00Z")
        val session = Session.create(UserId.new(), "hash", start, start.plusSeconds(3600))

        assertTrue(session.isActive(start.plusSeconds(10)))
        session.revoke(start.plusSeconds(20))
        assertFalse(session.isActive(start.plusSeconds(30)))
    }

    @Test
    fun recoveryTokenIsSingleUse() {
        val start = Instant.parse("2026-10-03T00:00:00Z")
        val token = RecoveryToken.create(UserId.new(), "hash", start, start.plusSeconds(3600))

        assertTrue(token.isUsable(start.plusSeconds(10)))
        token.consume(start.plusSeconds(20))
        assertFalse(token.isUsable(start.plusSeconds(30)))
    }
}
