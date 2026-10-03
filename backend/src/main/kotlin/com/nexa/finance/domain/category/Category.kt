package com.nexa.finance.domain.category

import com.nexa.finance.domain.common.DomainValidationException
import com.nexa.finance.domain.common.UserId
import java.time.Clock
import java.time.Instant
import java.util.UUID

@JvmInline
value class CategoryId(val value: UUID) {
    companion object {
        fun new(): CategoryId = CategoryId(UUID.randomUUID())
    }
}

enum class CategoryType {
    EXPENSE,
    INCOME,
    BOTH
}

enum class CategoryStatus {
    ACTIVE,
    ARCHIVED
}

/**
 * A financial category.
 *
 * ownerId == null means the category is owned by the system and is immutable
 * from the user's perspective. Non-null ownerId means a user-owned category.
 */
class Category private constructor(
    val id: CategoryId,
    val ownerId: UserId?,
    name: String,
    val type: CategoryType,
    val parentId: CategoryId?,
    iconKey: String?,
    val sortOrder: Int,
    val createdAt: Instant,
    updatedAt: Instant,
    status: CategoryStatus
) {
    val isSystem: Boolean
        get() = ownerId == null

    var name: String = validateName(name)
        private set

    var iconKey: String? = validateIconKey(iconKey)
        private set

    var updatedAt: Instant = updatedAt
        private set

    var status: CategoryStatus = status
        private set

    fun rename(newName: String, now: Instant) {
        ensureMutable()
        name = validateName(newName)
        updatedAt = now
    }

    fun changeIcon(newIconKey: String?, now: Instant) {
        ensureMutable()
        iconKey = validateIconKey(newIconKey)
        updatedAt = now
    }

    fun archive(now: Instant) {
        ensureMutable()
        if (status == CategoryStatus.ARCHIVED) return
        status = CategoryStatus.ARCHIVED
        updatedAt = now
    }

    fun restore(now: Instant) {
        ensureMutable()
        if (status == CategoryStatus.ACTIVE) return
        status = CategoryStatus.ACTIVE
        updatedAt = now
    }

    fun ensureUsable() {
        if (status == CategoryStatus.ARCHIVED) {
            throw DomainValidationException("Archived categories cannot be used for new transactions.")
        }
    }

    fun ensureBelongsTo(owner: UserId) {
        if (isSystem) return
        if (ownerId != owner) {
            throw DomainValidationException("Category does not belong to the user.")
        }
    }

    private fun ensureMutable() {
        if (isSystem) {
            throw DomainValidationException("System categories cannot be modified by users.")
        }
    }

    companion object {
        fun createSystem(
            name: String,
            type: CategoryType,
            parentId: CategoryId? = null,
            iconKey: String? = null,
            sortOrder: Int = 0,
            clock: Clock = Clock.systemUTC(),
            id: CategoryId = CategoryId.new()
        ): Category = createInternal(
            id = id,
            ownerId = null,
            name = name,
            type = type,
            parentId = parentId,
            iconKey = iconKey,
            sortOrder = sortOrder,
            clock = clock
        )

        fun createCustom(
            ownerId: UserId,
            name: String,
            type: CategoryType,
            parentId: CategoryId? = null,
            iconKey: String? = null,
            sortOrder: Int = 0,
            clock: Clock = Clock.systemUTC(),
            id: CategoryId = CategoryId.new()
        ): Category {
            return createInternal(
                id = id,
                ownerId = ownerId,
                name = name,
                type = type,
                parentId = parentId,
                iconKey = iconKey,
                sortOrder = sortOrder,
                clock = clock
            )
        }

        private fun createInternal(
            id: CategoryId,
            ownerId: UserId?,
            name: String,
            type: CategoryType,
            parentId: CategoryId?,
            iconKey: String?,
            sortOrder: Int,
            clock: Clock
        ): Category {
            if (parentId == id) {
                throw DomainValidationException("A category cannot be its own parent.")
            }
            if (sortOrder < 0) {
                throw DomainValidationException("Category sort order cannot be negative.")
            }

            val now = Instant.now(clock)
            return Category(
                id = id,
                ownerId = ownerId,
                name = name,
                type = type,
                parentId = parentId,
                iconKey = iconKey,
                sortOrder = sortOrder,
                createdAt = now,
                updatedAt = now,
                status = CategoryStatus.ACTIVE
            )
        }

        private fun validateName(value: String): String {
            val normalized = value.trim()
            if (normalized.length !in 1..60) {
                throw DomainValidationException("Category name must contain between 1 and 60 characters.")
            }
            return normalized
        }

        private fun validateIconKey(value: String?): String? {
            val normalized = value?.trim()?.takeIf { it.isNotEmpty() }
            if (normalized != null && normalized.length > 64) {
                throw DomainValidationException("Category icon key must contain at most 64 characters.")
            }
            return normalized
        }
    }
}
