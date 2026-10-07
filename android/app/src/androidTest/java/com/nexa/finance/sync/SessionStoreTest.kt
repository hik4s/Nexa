package com.nexa.finance.sync

import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class SessionStoreTest {
    private val store = SessionStore(ApplicationProvider.getApplicationContext())

    @Before
    fun clear() {
        store.clear()
    }

    @Test
    fun savesAndReadsEncryptedSession() {
        store.save("user-123", "token-secret")
        assertEquals("user-123", store.userId())
        assertEquals("token-secret", store.token())
    }

    @Test
    fun clearRemovesSession() {
        store.save("user-123", "token-secret")
        store.clear()
        assertNull(store.userId())
        assertNull(store.token())
    }
}
