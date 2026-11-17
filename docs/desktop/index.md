# Getting Started

Peekniko helps you inspect network requests and responses in your app.

## Configure Peekniko

Configure Peekniko once when your app starts, before creating your HTTP client:

```kotlin
import peekniko.Peekniko

Peekniko.configure {
    // Add any storage options here, or leave empty to use the defaults.
}
```

By default, Peekniko keeps captures in memory. To keep them between app launches, add [SQLDelight storage](sqldelight.md).

## Connect your HTTP client

- [OkHttp](okhttp.md)
- [Ktor](ktor.md)

## Open Peekniko

Peekniko's inspector is built with Compose Multiplatform, so your app needs the Compose Desktop runtime for its OS on the classpath. Compose Desktop apps already have it; other JVM apps can add it with the Compose Gradle plugin:

```kotlin title="build.gradle.kts"
dependencies {
    implementation(compose.desktop.currentOs)
}
```

Open the inspector in its own window:

```kotlin
import peekniko.Peekniko
import peekniko.open

Peekniko.open()
```

Or embed it in a Compose window you manage yourself:

```kotlin
import peekniko.Peekniko
import peekniko.ui.Screen

Window(onCloseRequest = { showPeekniko = false }, title = "Peekniko") {
    Peekniko.Screen(onClose = { showPeekniko = false })
}
```

Peekniko also adds a system tray icon that shows new calls and opens the inspector.
