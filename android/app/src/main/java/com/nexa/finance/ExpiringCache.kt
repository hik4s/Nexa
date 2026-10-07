package com.nexa.finance

class ExpiringCache<K, V>(
    private val ttlMillis: Long,
    private val nowMillis: () -> Long = { System.currentTimeMillis() },
) {
    init {
        require(ttlMillis > 0) { "ttlMillis must be greater than zero." }
    }

    private data class Entry<V>(
        val value: V,
        val expiresAtMillis: Long,
    )

    private val entries = mutableMapOf<K, Entry<V>>()

    @Synchronized
    fun get(key: K): V? {
        val entry = entries[key] ?: return null
        if (nowMillis() >= entry.expiresAtMillis) {
            entries.remove(key)
            return null
        }
        return entry.value
    }

    @Synchronized
    fun put(key: K, value: V) {
        entries[key] = Entry(
            value = value,
            expiresAtMillis = nowMillis() + ttlMillis,
        )
    }

    @Synchronized
    fun invalidate(key: K) {
        entries.remove(key)
    }

    @Synchronized
    fun clear() {
        entries.clear()
    }
}
