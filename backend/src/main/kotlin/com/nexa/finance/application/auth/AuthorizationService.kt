package com.nexa.finance.application.auth

import com.nexa.finance.domain.common.DomainValidationException
import com.nexa.finance.domain.common.UserId
import com.nexa.finance.domain.user.UserStatus

class AuthorizationService {
    fun requireOwner(currentUserId: UserId, resourceOwnerId: UserId) {
        if (currentUserId != resourceOwnerId) {
            throw DomainValidationException("Access to this resource is denied.")
        }
    }

    fun requireActiveUser(status: UserStatus) {
        if (status != UserStatus.ACTIVE) {
            throw DomainValidationException("User is not authorized for this operation.")
        }
    }
}
