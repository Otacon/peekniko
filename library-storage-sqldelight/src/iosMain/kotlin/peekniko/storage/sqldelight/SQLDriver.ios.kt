package peekniko.storage.sqldelight

import app.cash.sqldelight.driver.native.NativeSqliteDriver
import peekniko.Peekniko

public actual fun Peekniko.Configuration.sqlDelightStorage(name: String) {
    val driver = NativeSqliteDriver(DestructiveMigrationSchema, name)
    storage = SqlDelightNetworkRepository(driver)
}
