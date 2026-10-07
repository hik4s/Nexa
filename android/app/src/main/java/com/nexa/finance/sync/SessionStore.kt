package com.nexa.finance.sync

import android.content.Context
import android.util.Base64
import java.nio.ByteBuffer
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class SessionStore(context: Context) {
    private val prefs = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)

    fun save(userId: String, token: String) {
        prefs.edit().putString(KEY_USER_ID, userId).putString(KEY_TOKEN, encrypt(token)).apply()
    }

    fun userId(): String? = prefs.getString(KEY_USER_ID, null)
    fun token(): String? = prefs.getString(KEY_TOKEN, null)?.let(::decrypt)
    fun clear() { prefs.edit().clear().apply() }

    private fun encrypt(value: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey())
        val iv = cipher.iv
        val ciphertext = cipher.doFinal(value.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(
            ByteBuffer.allocate(4 + iv.size + ciphertext.size).putInt(iv.size).put(iv).put(ciphertext).array(),
            Base64.NO_WRAP,
        )
    }

    private fun decrypt(value: String): String {
        val bytes = Base64.decode(value, Base64.NO_WRAP)
        val buffer = ByteBuffer.wrap(bytes)
        val ivSize = buffer.int
        require(ivSize in 12..32) { "Invalid encrypted session." }
        val iv = ByteArray(ivSize).also(buffer::get)
        val ciphertext = ByteArray(buffer.remaining()).also(buffer::get)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(128, iv))
        return String(cipher.doFinal(ciphertext), Charsets.UTF_8)
    }

    private fun secretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KEY_ALGORITHM, ANDROID_KEYSTORE)
        generator.init(
            android.security.keystore.KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                android.security.keystore.KeyProperties.PURPOSE_ENCRYPT or
                    android.security.keystore.KeyProperties.PURPOSE_DECRYPT,
            ).setBlockModes(android.security.keystore.KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(android.security.keystore.KeyProperties.ENCRYPTION_PADDING_NONE)
                .setUserAuthenticationRequired(false)
                .build(),
        )
        return generator.generateKey()
    }

    companion object {
        private const val PREFERENCES = "nexa_session"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_TOKEN = "token"
        private const val KEY_ALIAS = "nexa_session_key"
        private const val KEY_ALGORITHM = "AES"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
    }
}
