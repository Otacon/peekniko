package peekniko.storage.sqldelight

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import peekniko.storage.sqldelight.db.PeeknikoDatabase

internal actual fun createTestSqlDriver(name: String): SqlDriver = NativeSqliteDriver(
    schema = PeeknikoDatabase.Schema,
    name = name,
    onConfiguration = { it.copy(inMemory = true) },
)
