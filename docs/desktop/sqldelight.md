# SQLDelight storage

Keep your captured requests and responses between app launches with SQLDelight storage.

## Gradle dependencies

Add the Peekniko core and storage adapter, plus SQLDelight's:

- runtime
- coroutine extensions
- SQLite driver

Use the same Peekniko version for both artifacts and the same SQLDelight version for all SQLDelight artifacts:

```kotlin title="build.gradle.kts"
dependencies {
    // Debug builds
    implementation("io.github.otacon.peekniko:client-core:<peekniko-version>")
    implementation("io.github.otacon.peekniko:storage-sqldelight:<peekniko-version>")
    implementation("app.cash.sqldelight:runtime:<sqldelight-version>")
    implementation("app.cash.sqldelight:coroutines-extensions:<sqldelight-version>")
    implementation("app.cash.sqldelight:sqlite-driver:<sqldelight-version>")
}
```

The inspector needs the Swing coroutine dispatcher. Use the same version as your other coroutine dependencies.

## Configure Peekniko and storage

Add storage to your startup configuration before creating your HTTP client:

```kotlin
import peekniko.Peekniko
import peekniko.storage.sqldelight.sqlDelightStorage

Peekniko.configure {
    sqlDelightStorage(name = "peekniko.db")
}
```

Use a filename or path for the database. Peekniko creates it for you if it doesn't exist yet.
