# Getting Started

Peekniko helps you inspect network requests and responses in your app.

## Swift Package Manager

In a Swift-only app, add `https://github.com/Otacon/peekniko` as a package dependency, pick a release version, and add
the `Peekniko` product to your app target. Then `import Peekniko`.

Debug builds get the inspector, with `URLSession` capture and SQLDelight storage. Every other build configuration gets a
no-op with the same API, so the same code compiles everywhere and needs no `#if` around Peekniko calls.

To keep the inspector out of release builds entirely, add two build settings to the app target's **Release**
configuration. Without them a release build still behaves as a no-op, but it carries the unused framework:

| Build setting                               | Value                    |
|---------------------------------------------|--------------------------|
| Excluded Source File Names (`EXCLUDED_SOURCE_FILE_NAMES`) | `PeeknikoCore.framework` |
| Other Linker Flags (`OTHER_LDFLAGS`)        | `$(inherited) -Wl,-dead_strip_dylibs` |

Both are needed: the first stops Xcode embedding the framework, the second removes the app's reference to it, without
which the app fails to launch.

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
