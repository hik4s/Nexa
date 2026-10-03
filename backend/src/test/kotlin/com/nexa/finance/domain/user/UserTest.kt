package com.nexa.finance.domain.user

import com.nexa.finance.domain.common.DomainValidationException
import com.nexa.finance.domain.common.UserId
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class UserTest {
    private val clock = Clock.fixed(Instant.parse("2026-10-03T00:00:00Z"), ZoneOffset.UTC)

    @Test
    fun normalizesEmail() {
        val user = User.create("  KAUAN@Example.COM ", "Kauan", clock = clock)
        assertEquals("kauan@example.com", user.email)
    }

    @Test
    fun rejectsInvalidEmail() {
        assertFailsWith<DomainValidationException> {
            User.create("invalid", "Kauan", clock = clock)
        }
    }

    @Test
    fun deactivatedUserCannotBeModified() {
        val user = User.create("kauan@example.com", "Kauan", clock = clock, id = UserId.new())
        user.deactivate(Instant.now(clock))
        assertFailsWith<DomainValidationException> {
            user.changeDisplayName("Outro", Instant.now(clock))
        }
    }
}
