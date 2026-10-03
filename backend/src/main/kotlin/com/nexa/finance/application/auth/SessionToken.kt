package com.nexa.finance.application.auth

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

data class SessionToken(val raw: String, val hash: String) {
    companion object {
        private val RANDOM = SecureRandom()
        private val ENCODER = Base64.getUrlEncoder().withoutPadding()

        fun generate(): SessionToken {
            val bytes = ByteArray(32)
            RANDOM.nextBytes(bytes)
            val raw = ENCODER.encodeToString(bytes)
            return SessionToken(raw, sha256(raw))
        }

        fun hash(raw: String): String = sha256(raw)

        private fun sha256(value: String): String =
            MessageDigest.getInstance("SHA-256")
                .digest(value.toByteArray(Charsets.UTF_8))
                .joinToString("") { "%02x".format(it) }
    }
}
