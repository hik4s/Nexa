package com.nexa.finance.domain.user

import com.nexa.finance.domain.common.DomainValidationException
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

class PasswordCredential private constructor(
    val encodedHash: String,
    val algorithm: String,
) {
    fun matches(password: CharArray): Boolean {
        val parts = encodedHash.split('$')
        if (parts.size != 5 || parts[0] != "pbkdf2" || parts[1] != "sha256") return false
        val iterations = parts[2].toIntOrNull() ?: return false
        val salt = decode(parts[3]) ?: return false
        val expected = decode(parts[4]) ?: return false
        val actual = derive(password, salt, iterations, expected.size * 8)
        return MessageDigest.isEqual(actual, expected)
    }

    companion object {
        private val PREFIX = "pbkdf2" + '$' + "sha256"
        private const val ALGORITHM = "PBKDF2WithHmacSHA256"
        private const val ITERATIONS = 600_000
        private const val SALT_BYTES = 16
        private const val KEY_BITS = 256
        private val RANDOM = SecureRandom()
        private val ENCODER = Base64.getUrlEncoder().withoutPadding()
        private val DECODER = Base64.getUrlDecoder()

        fun create(password: CharArray): PasswordCredential {
            validatePassword(password)
            val salt = ByteArray(SALT_BYTES)
            RANDOM.nextBytes(salt)
            val hash = derive(password, salt, ITERATIONS, KEY_BITS)
            return PasswordCredential(
                encodedHash = PREFIX + '$' + ITERATIONS + '$' + encode(salt) + '$' + encode(hash),
                algorithm = ALGORITHM,
            )
        }

        fun restore(encodedHash: String, algorithm: String): PasswordCredential =
            PasswordCredential(encodedHash, algorithm)

        private fun validatePassword(password: CharArray) {
            if (password.size !in 8..128) {
                throw DomainValidationException("Password must contain between 8 and 128 characters.")
            }
        }

        private fun derive(password: CharArray, salt: ByteArray, iterations: Int, keyBits: Int): ByteArray {
            val spec = PBEKeySpec(password, salt, iterations, keyBits)
            return try {
                SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).encoded
            } finally {
                spec.clearPassword()
            }
        }

        private fun encode(value: ByteArray): String = ENCODER.encodeToString(value)
        private fun decode(value: String): ByteArray? = runCatching { DECODER.decode(value) }.getOrNull()
    }
}
