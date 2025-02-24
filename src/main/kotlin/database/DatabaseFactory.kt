package database

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.sql.Database

object DatabaseFactory {

    fun createAndConnect() {
        val config = HikariConfig()

        config.jdbcUrl = "jdbc:postgresql://localhost:5434/LetThemCook"
        config.driverClassName = "org.postgresql.Driver"
        config.username = "postgres"
        config.password = "admin"
        config.maximumPoolSize = 10
        config.isAutoCommit = false
        config.transactionIsolation = "TRANSACTION_REPEATABLE_READ"
        config.validate()

        val dataSource = HikariDataSource(config)
        Database.connect(dataSource)
    }
}