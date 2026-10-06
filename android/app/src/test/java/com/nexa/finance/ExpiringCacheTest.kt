package com.nexa.finance

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class ExpiringCacheTest {
    @Test
    fun returnsValueUntilTtlExpires() {
        var now = 1_000L
        val cache = ExpiringCache<String, String>(
            ttlMillis = 1_000L,
            nowMillis = { now },
        )

        cache.put("key", "value")
        assertEquals("value", cache.get("key"))

        now = 1_999L
        assertEquals("value", cache.get("key"))

        now = 2_000L
        assertNull(cache.get("key"))
    }

    @Test
    fun invalidationAndClearRemoveEntries() {
        val cache = ExpiringCache<String, Int>(
            ttlMillis = 10_000L,
            nowMillis = { 0L },
        )

        cache.put("a", 1)
        cache.put("b", 2)
        cache.invalidate("a")
        assertNull(cache.get("a"))
        assertEquals(2, cache.get("b"))

        cache.clear()
        assertNull(cache.get("b"))
    }

    @Test
    fun rejectsNonPositiveTtl() {
        assertFailsWith<IllegalArgumentException> {
            ExpiringCache<String, String>(ttlMillis = 0)
        }
    }
}
