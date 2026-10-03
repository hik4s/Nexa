package com.nexa.finance.infrastructure.database

import java.sql.Connection
import java.sql.DriverManager

class DatabaseConnectionProvider(
    private val settingsProvider: () -> DatabaseSettings = DatabaseSettings::fromEnvironment,
) {
    fun open(): Connection {
        val settings = settingsProvider()
        return DriverManager.getConnection(settings.jdbcUrl, settings.username, settings.password)
    }
}
