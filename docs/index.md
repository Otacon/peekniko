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

By default, Peekniko keeps captures in memory. To keep them between app launches, add [SQLDelight storage](android/sqldelight.md).

## Connect your HTTP client

- [OkHttp](android/okhttp.md)
- [Ktor](android/ktor.md)

## Open Peekniko

```kotlin
import peekniko.Peekniko
import peekniko.open

Peekniko.open(context)
```

Peekniko also posts a quiet notification listing the latest calls; tapping it opens the inspector. It asks for the notification permission the first time its screen opens, unless your app has already asked.
