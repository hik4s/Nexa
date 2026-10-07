package com.nexa.finance.application.auth

import com.nexa.finance.domain.common.DomainValidationException
import com.nexa.finance.domain.common.UserId
import com.nexa.finance.domain.user.UserStatus
import kotlin.test.Test
import kotlin.test.assertFailsWith

class AuthorizationServiceTest {
    private val authorization = AuthorizationService()

    @Test
    fun ownerCanAccessOwnedResource() {
        val userId = UserId.new()
        authorization.requireOwner(userId, userId)
    }

    @Test
    fun differentUserCannotAccessResource() {
        assertFailsWith<DomainValidationException> {
            authorization.requireOwner(UserId.new(), UserId.new())
        }
    }

    @Test
    fun onlyActiveUsersAreAuthorized() {
        authorization.requireActiveUser(UserStatus.ACTIVE)

        assertFailsWith<DomainValidationException> {
            authorization.requireActiveUser(UserStatus.SUSPENDED)
        }
        assertFailsWith<DomainValidationException> {
            authorization.requireActiveUser(UserStatus.DEACTIVATED)
        }
    }
}
