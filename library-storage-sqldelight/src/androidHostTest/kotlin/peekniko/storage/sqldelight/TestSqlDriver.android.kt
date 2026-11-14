package peekniko.storage.sqldelight

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import peekniko.storage.sqldelight.db.PeeknikoDatabase

internal actual fun createTestSqlDriver(name: String): SqlDriver =
    JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).also { PeeknikoDatabase.Schema.create(it) }
