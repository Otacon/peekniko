package peekniko.storage.sqldelight

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import app.cash.sqldelight.db.QueryResult
import peekniko.Peekniko
import java.io.File

public actual fun Peekniko.Configuration.sqlDelightStorage(name: String) {
    val databaseFile = File(name)
    val isNewDatabase = !databaseFile.exists()
    val driver = JdbcSqliteDriver("jdbc:sqlite:${databaseFile.absolutePath}")
    if (isNewDatabase) {
        DestructiveMigrationSchema.create(driver)
    } else {
        val oldVersion = driver.executeQuery(
            identifier = null,
            sql = "PRAGMA user_version",
            mapper = { cursor -> QueryResult.Value(cursor.getLong(0) ?: 0L) },
            parameters = 0,
        ).value
        if (oldVersion < DestructiveMigrationSchema.version) {
            DestructiveMigrationSchema.migrate(driver, oldVersion, DestructiveMigrationSchema.version)
        }
    }
    driver.execute(null, "PRAGMA user_version = ${DestructiveMigrationSchema.version}", 0)
    storage = SqlDelightNetworkRepository(driver)
}
