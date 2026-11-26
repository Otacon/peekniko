# Getting Started

Peekniko helps you inspect network requests and responses in your app.

## Swift Package Manager

In a Swift-only app, add `https://github.com/Otacon/peekniko` as a package dependency and pick a release version.
The package has two products with the same API:

| Product           | Import             | Contents                                                    | Link it into                  |
|-------------------|--------------------|-------------------------------------------------------------|-------------------------------|
| `PeeknikoKit`     | `PeeknikoCore`     | The inspector, `URLSession` capture, and SQLDelight storage | Debug or internal app targets |
| `PeeknikoKitNoop` | `PeeknikoCoreNoop` | The same API, doing nothing, in a few KB of plain Swift     | Production app targets        |

Link one of the two into each app target. Swift Package Manager can't switch products by build configuration, so use a
separate app target for debug builds. `PeeknikoKit` links SQLite for you.

To share code between the targets, add a compilation condition such as `PEEKNIKO` to the debug target's
**Active Compilation Conditions** and import whichever module is linked:

```swift
#if PEEKNIKO
import PeeknikoCore
#else
import PeeknikoCoreNoop
#endif
```

In a Kotlin Multiplatform app, add the Gradle dependencies from each page instead.

## Configure Peekniko

Configure Peekniko once when your app starts, before creating your HTTP client:

```kotlin
import peekniko.Peekniko

Peekniko.configure {
    // Add any storage options here, or leave empty to use the defaults.
}
```

By default, Peekniko keeps captures in memory. To keep them between app launches, add [SQLDelight storage](sqldelight.md).

From Swift:

```swift
Peekniko.shared.configure { configuration in
    configuration.inMemoryStorage(maxApiCalls: 100)
}
```

## Connect your HTTP client

- [Ktor](ktor.md)
- [URLSession](urlsession.md)

## Open Peekniko

The inspector is a Compose Multiplatform screen. Present it from the main thread:

```kotlin
import peekniko.Peekniko
import peekniko.open

Peekniko.open()
```

From Swift, with the framework exporting Peekniko:

```swift
Peekniko.shared.open(from: nil)
```

To embed it in your own Compose UI, use `Peekniko.Screen()`.

## Notifications

Peekniko posts a quiet notification listing the latest calls once notifications are authorized. It asks for permission
the first time its screen opens, unless your app has already asked.

If your app sets its own `UNUserNotificationCenterDelegate`, forward to Peekniko:

```swift
func userNotificationCenter(_ center: UNUserNotificationCenter, willPresent notification: UNNotification) async -> UNNotificationPresentationOptions {
    Peekniko.shared.isPeeknikoNotification(notification: notification) ? [.list] : []
}

func userNotificationCenter(_ center: UNUserNotificationCenter, didReceive response: UNNotificationResponse) async {
    _ = Peekniko.shared.handleNotificationResponse(response: response)
}
```

Without a delegate of your own, no setup is needed.
