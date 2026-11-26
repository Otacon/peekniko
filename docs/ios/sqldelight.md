# SQLDelight storage

Keep your captured requests and responses between app launches with SQLDelight storage.

In a Swift-only app, SQLDelight storage is already part of the `Peekniko` product from
[Swift Package Manager](index.md#swift-package-manager), which also links SQLite. Skip to
[Configure Peekniko and storage](#configure-peekniko-and-storage).

## Gradle dependencies

```kotlin title="build.gradle.kts"
kotlin {
    sourceSets {
        iosMain.dependencies {
            implementation("io.github.otacon.peekniko:storage-sqldelight:<peekniko-version>")
        }
    }
}
```

If your app already uses SQLDelight, no additional SQLDelight dependencies are needed. Otherwise, add the runtime, coroutine extensions, and native driver, using the same SQLDelight version for all three:

```kotlin title="build.gradle.kts"
kotlin {
    sourceSets {
        iosMain.dependencies {
            implementation("app.cash.sqldelight:runtime:<sqldelight-version>")
            implementation("app.cash.sqldelight:coroutines-extensions:<sqldelight-version>")
            implementation("app.cash.sqldelight:native-driver:<sqldelight-version>")
        }
    }
}
```

Link SQLite in your iOS app by adding `-lsqlite3` to **Other Linker Flags**.

## Configure Peekniko and storage

Add storage to your startup configuration before creating your HTTP client.

```kotlin
import peekniko.Peekniko
import peekniko.storage.sqldelight.sqlDelightStorage

Peekniko.configure {
    sqlDelightStorage(name = "peekniko.db")
}
```

Or from Swift, with the framework exporting the storage module:

```swift
Peekniko.shared.configure { configuration in
    configuration.sqlDelightStorage(name: "peekniko.db")
}
```
