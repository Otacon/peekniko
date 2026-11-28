# Getting Started

Peekniko helps you inspect network requests and responses in your app.

## Install

### Swift Package Manager

Requires iOS 17 or later.

In Xcode, choose **File > Add Package Dependencies…**, enter the repository URL, pick a release and add the
`Peekniko` product to your app target:

```text
https://github.com/Otacon/peekniko
```

Or in a `Package.swift`:

```swift title="Package.swift"
dependencies: [
    .package(url: "https://github.com/Otacon/peekniko", exact: "<peekniko-version>"),
],
targets: [
    .target(name: "MyApp", dependencies: [.product(name: "Peekniko", package: "peekniko")]),
]
```

In your app target's build settings, for the **Release** configuration, set both:

| Build setting | Value |
|---|---|
| Excluded Source File Names (`EXCLUDED_SOURCE_FILE_NAMES`) | `PeeknikoCore.framework` |
| Other Linker Flags (`OTHER_LDFLAGS`) | `$(inherited) -Wl,-dead_strip_dylibs` |

### Kotlin Multiplatform

Add Peekniko to your shared module with Gradle, as shown on the [Ktor](ktor.md), [URLSession](urlsession.md) and
[SQLDelight](sqldelight.md) pages.

## Configure Peekniko

Configure Peekniko once when your app starts, before creating your HTTP client:

```swift
import Peekniko

Peekniko.shared.configure { configuration in
    configuration.inMemoryStorage(maxApiCalls: 100)
}
```

From Kotlin:

```kotlin
import peekniko.Peekniko

Peekniko.configure {
    // Add any storage options here, or leave empty to use the defaults.
}
```

To keep captures between app launches, use [SQLDelight storage](sqldelight.md).

## Connect your HTTP client

- [URLSession](urlsession.md)
- [Ktor](ktor.md)

## Open Peekniko

From the main thread:

```swift
Peekniko.shared.open(from: nil)
```

From Kotlin:

```kotlin
import peekniko.Peekniko
import peekniko.open

Peekniko.open()
```

To embed it in your own Compose UI, use `Peekniko.Screen()`.

## Notifications

Peekniko shows a notification with the latest calls. Tapping it opens the inspector.

### Permission

Peekniko asks for notification permission the first time its screen opens. To show the notification from the first
call, ask for permission when your app starts:

```swift
try? await UNUserNotificationCenter.current().requestAuthorization(options: [.alert])
```

### Notification delegate

If your app sets its own `UNUserNotificationCenterDelegate`, forward Peekniko's notifications from it. In a SwiftUI
app:

```swift
import Peekniko
import SwiftUI
import UserNotifications

@main
struct MyApp: App {
    @UIApplicationDelegateAdaptor private var appDelegate: AppDelegate

    var body: some Scene {
        WindowGroup { ContentView() }
    }
}

final class AppDelegate: NSObject, UIApplicationDelegate, @preconcurrency UNUserNotificationCenterDelegate {
    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil
    ) -> Bool {
        UNUserNotificationCenter.current().delegate = self
        return true
    }

    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        willPresent notification: UNNotification,
        withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void
    ) {
        if Peekniko.shared.isPeeknikoNotification(notification: notification) {
            completionHandler([.list])
            return
        }
        completionHandler([.banner, .sound]) // Your app's own notifications.
    }

    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        didReceive response: UNNotificationResponse,
        withCompletionHandler completionHandler: @escaping () -> Void
    ) {
        defer { completionHandler() }
        if Peekniko.shared.handleNotificationResponse(response: response) {
            return
        }
        // Handle your app's own notifications.
    }
}
```

Without a delegate of your own, no setup is needed.
