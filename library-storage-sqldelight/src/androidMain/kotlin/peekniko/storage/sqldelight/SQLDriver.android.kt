package peekniko.storage.sqldelight

import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import peekniko.Peekniko
import peekniko.context

public actual fun Peekniko.Configuration.sqlDelightStorage(name: String) {
    val driver = AndroidSqliteDriver(DestructiveMigrationSchema, context, name)
    storage = SqlDelightNetworkRepository(driver)
}
