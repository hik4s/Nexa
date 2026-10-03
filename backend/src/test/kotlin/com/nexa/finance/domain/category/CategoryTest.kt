package com.nexa.finance.domain.category

import com.nexa.finance.domain.common.DomainValidationException
import com.nexa.finance.domain.common.UserId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class CategoryTest {
    private val fixedClock = Clock.fixed(Instant.parse("2026-10-03T12:00:00Z"), ZoneOffset.UTC)
    private val owner = UserId.new()

    @Test
    fun createsCustomCategoryAndNormalizesFields() {
        val category = Category.createCustom(
            ownerId = owner,
            name = "  Alimentação  ",
            type = CategoryType.EXPENSE,
            iconKey = " food ",
            sortOrder = 2,
            clock = fixedClock
        )

        assertEquals("Alimentação", category.name)
        assertEquals(CategoryType.EXPENSE, category.type)
        assertEquals("food", category.iconKey)
        assertEquals(2, category.sortOrder)
        assertEquals(CategoryStatus.ACTIVE, category.status)
        assertEquals(owner, category.ownerId)
        assertEquals(Instant.parse("2026-10-03T12:00:00Z"), category.createdAt)
    }

    @Test
    fun systemCategoryIsImmutableForUsers() {
        val category = Category.createSystem(
            name = "Alimentação",
            type = CategoryType.EXPENSE,
            clock = fixedClock
        )

        assertEquals(true, category.isSystem)
        assertFailsWith<DomainValidationException> {
            category.rename("Comida", Instant.parse("2026-10-03T13:00:00Z"))
        }
        assertFailsWith<DomainValidationException> {
            category.archive(Instant.parse("2026-10-03T13:00:00Z"))
        }
    }

    @Test
    fun archivedCustomCategoryCannotBeUsedButCanBeRestored() {
        val category = Category.createCustom(owner, "Lazer", CategoryType.EXPENSE, clock = fixedClock)
        category.archive(Instant.parse("2026-10-03T13:00:00Z"))

        assertEquals(CategoryStatus.ARCHIVED, category.status)
        assertFailsWith<DomainValidationException> { category.ensureUsable() }

        category.restore(Instant.parse("2026-10-03T14:00:00Z"))
        category.ensureUsable()
        assertEquals(CategoryStatus.ACTIVE, category.status)
    }

    @Test
    fun rejectsSelfParentAndNegativeSortOrder() {
        val id = CategoryId.new()

        assertFailsWith<DomainValidationException> {
            Category.createCustom(owner, "Teste", CategoryType.BOTH, parentId = id, id = id, clock = fixedClock)
        }
        assertFailsWith<DomainValidationException> {
            Category.createCustom(owner, "Teste", CategoryType.BOTH, sortOrder = -1, clock = fixedClock)
        }
    }

    @Test
    fun normalizesBlankIconToNull() {
        val category = Category.createCustom(owner, "Compras", CategoryType.EXPENSE, iconKey = "   ", clock = fixedClock)
        assertNull(category.iconKey)
    }
}
