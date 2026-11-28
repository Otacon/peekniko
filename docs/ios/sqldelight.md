# SQLDelight storage

Keep your captured requests and responses between app launches with SQLDelight storage.

## Gradle dependencies

With the Swift package, skip to [configuring the storage](#configure-peekniko-and-storage).

In a Kotlin Multiplatform app, add the storage module to your shared module:

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

From Swift:

```swift
Peekniko.shared.configure { configuration in
    configuration.sqlDelightStorage(name: "peekniko.db")
}
```
