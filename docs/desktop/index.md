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

```kotlin
import peekniko.ui.openPeeknikoWindow

openPeeknikoWindow()
```

Peekniko also adds a system tray icon that shows new calls and opens the inspector.
