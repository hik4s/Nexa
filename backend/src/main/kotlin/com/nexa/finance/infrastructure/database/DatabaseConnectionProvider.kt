package com.nexa.finance.infrastructure.database

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import java.sql.Connection

class DatabaseConnectionProvider(
    private val settingsProvider: () -> DatabaseSettings = DatabaseSettings::fromEnvironment,
    private val poolSettingsProvider: () -> DatabasePoolSettings = DatabasePoolSettings::fromEnvironment,
) : AutoCloseable {
    private var dataSource: HikariDataSource? = null

    fun open(): Connection = synchronized(this) {
        dataSource().connection
    }

    override fun close() {
        synchronized(this) {
            dataSource?.close()
            dataSource = null
        }
    }

    private fun dataSource(): HikariDataSource = dataSource ?: createDataSource().also { dataSource = it }

    private fun createDataSource(): HikariDataSource {
        val database = settingsProvider()
        val pool = poolSettingsProvider()
        val config = HikariConfig().apply {
            jdbcUrl = database.jdbcUrl
            username = database.username
            password = database.password
            maximumPoolSize = pool.maximumPoolSize
            minimumIdle = pool.minimumIdle
            connectionTimeout = 5_000
            validationTimeout = 3_000
            keepaliveTime = 300_000
            addDataSourceProperty("tcpKeepAlive", "true")
            poolName = "NexaPostgresPool"
        }
        return HikariDataSource(config)
    }
}
