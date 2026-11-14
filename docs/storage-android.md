# Android — SQLDelight storage

Keep your captured requests and responses between app launches with SQLDelight storage.

## Gradle dependencies

Add the Peekniko core and storage adapter, plus SQLDelight's:

- runtime
- coroutine extensions
- Android driver

Use the same Peekniko version for both artifacts and the same SQLDelight version for all SQLDelight artifacts:

```kotlin title="build.gradle.kts"
dependencies {
    debugImplementation("io.github.otacon.peekniko:storage-sqldelight:<peekniko-version>")
    releaseImplementation("io.github.otacon.peekniko:client-noop:<peekniko-version>")

    implementation("app.cash.sqldelight:runtime:<sqldelight-version>")
    implementation("app.cash.sqldelight:coroutines-extensions:<sqldelight-version>")
    implementation("app.cash.sqldelight:android-driver:<sqldelight-version>")
}
```

## Configure Peekniko and storage

Add storage to your startup configuration before creating your HTTP client.

```kotlin
import peekniko.Peekniko
import peekniko.storage.sqldelight.sqlDelightStorage

Peekniko.configure {
    sqlDelightStorage(name = "peekniko.db")
}
```

Peekniko saves the database in your app's database directory using the name you provide.
