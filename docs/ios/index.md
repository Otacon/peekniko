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
import peekniko.present

Peekniko.present()
```

From Swift, with the framework exporting Peekniko:

```swift
Peekniko.shared.present(from: nil)
```

To embed it yourself, use `Peekniko.viewController(onClose)` or, inside Compose, `Peekniko.Screen()`.

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
