package com.nexa.finance.domain.common

import java.util.UUID

@JvmInline
value class UserId(val value: UUID) {
    companion object {
        fun new(): UserId = UserId(UUID.randomUUID())
    }
}
