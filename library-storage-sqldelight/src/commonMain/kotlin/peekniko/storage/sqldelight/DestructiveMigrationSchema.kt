package peekniko.storage.sqldelight

import app.cash.sqldelight.db.AfterVersion
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import peekniko.storage.sqldelight.db.PeeknikoDatabase

internal object DestructiveMigrationSchema : SqlSchema<QueryResult.Value<Unit>> by PeeknikoDatabase.Schema {
    override fun migrate(
        driver: SqlDriver,
        oldVersion: Long,
        newVersion: Long,
        vararg callbacks: AfterVersion,
    ): QueryResult.Value<Unit> {
        val tables = driver.executeQuery(
            identifier = null,
            sql = "SELECT name FROM sqlite_master WHERE type = 'table'",
            parameters = 0,
            mapper = { cursor ->
                QueryResult.Value(buildList {
                    while (cursor.next().value) {
                        val tableName = cursor.getString(0)!!
                        if (tableName != "sqlite_sequence" && tableName != "android_metadata") {
                            add(tableName)
                        }
                    }
                })
            },
        ).value

        tables.forEach { tableName ->
            val escapedName = tableName.replace("\"", "\"\"")
            driver.execute(null, "DROP TABLE \"$escapedName\"", 0)
        }

        return create(driver)
    }
}
