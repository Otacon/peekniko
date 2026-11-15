# Android — SQLDelight storage

Keep your captured requests and responses between app launches with SQLDelight storage.

## Gradle dependencies

Add the Peekniko storage adapter for debug builds and the no-op client for release builds. Use the same Peekniko version for both artifacts:

```kotlin title="build.gradle.kts"
dependencies {
    debugImplementation("io.github.otacon.peekniko:storage-sqldelight:<peekniko-version>")
    releaseImplementation("io.github.otacon.peekniko:client-noop:<peekniko-version>")
}
```

If your app already uses SQLDelight, no additional SQLDelight dependencies are needed. Otherwise, add the runtime, coroutine extensions, and Android driver to debug builds, using the same SQLDelight version for all three:

```kotlin title="build.gradle.kts"
dependencies {
    debugImplementation("app.cash.sqldelight:runtime:<sqldelight-version>")
    debugImplementation("app.cash.sqldelight:coroutines-extensions:<sqldelight-version>")
    debugImplementation("app.cash.sqldelight:android-driver:<sqldelight-version>")
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
