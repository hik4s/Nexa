package com.nexa.finance.infrastructure.database

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class DatabaseSettingsTest {

    @Test
    fun readsPasswordFromEnvironment() {
        val settings = DatabaseSettings.fromEnvironment(
            mapOf(
                "POSTGRES_HOST" to "db.example",
                "POSTGRES_PORT" to "5432",
                "POSTGRES_DB" to "nexa",
                "POSTGRES_USER" to "nexa",
                "POSTGRES_PASSWORD" to "secret",
            )
        )

        assertEquals("jdbc:postgresql://db.example:5432/nexa", settings.jdbcUrl)
        assertEquals("nexa", settings.username)
        assertEquals("secret", settings.password)
    }

    @Test
    fun readsPasswordFromSecretFile() {
        val file = Files.createTempFile("nexa-db-password-", ".secret")
        try {
            Files.writeString(file, "file-secret\r\n")

            val settings = DatabaseSettings.fromEnvironment(
                mapOf(
                    "POSTGRES_PASSWORD_FILE" to file.toString(),
                )
            )

            assertEquals("file-secret", settings.password)
        } finally {
            Files.deleteIfExists(file)
        }
    }

    @Test
    fun rejectsBothPasswordSources() {
        val exception = assertFailsWith<IllegalStateException> {
            DatabaseSettings.fromEnvironment(
                mapOf(
                    "POSTGRES_PASSWORD" to "secret",
                    "POSTGRES_PASSWORD_FILE" to "password-file",
                )
            )
        }

        assertTrue(exception.message!!.contains("POSTGRES_PASSWORD"))
        assertTrue(exception.message!!.contains("POSTGRES_PASSWORD_FILE"))
    }

    @Test
    fun rejectsMissingPassword() {
        val exception = assertFailsWith<IllegalStateException> {
            DatabaseSettings.fromEnvironment(emptyMap())
        }

        assertTrue(exception.message!!.contains("POSTGRES_PASSWORD"))
    }
}
