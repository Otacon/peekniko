# iOS app

Open `PeeknikoApp.xcodeproj` in Xcode, select an iOS 17+ simulator or device, and run the `PeeknikoApp` scheme. The Xcode build phase builds the matching Kotlin `Peekniko.framework` from the repository's `app` module, and the Swift entry point in `PeeknikoApp.swift` only hosts the shared Compose UI returned by `MainViewController()`.

The shared iOS app target uses Ktor's Darwin engine. The OkHttp action is only shown on Android/JVM.

The sample app registers global `URLSession` inspection in `PeeknikoApp.init`. Tap **Run URLSession requests**, then open Peekniko to inspect it. In another host app, register once during startup, before creating requests:

```swift
Peekniko.shared.configure { configuration in
    configuration.inMemoryStorage(maxApiCalls: 100)
}
Peekniko.shared.registerUrlProtocol()
```

`Peekniko.shared.unregisterUrlProtocol()` disables capture, and `Peekniko.shared.addUrlProtocol(configuration:)` enables it for sessions created from their own `URLSessionConfiguration`. Global registration makes the protocol available to the URL loading system, but Apple doesn’t guarantee every registered protocol will be consulted. Background sessions and networking stacks that bypass `URLProtocol` aren’t captured. The protocol forwards requests through `URLSession.shared`, so custom per-session proxy, cache, cookie, or TLS trust settings aren’t carried over.

## Inspector UI

The inspector is the library's Compose Multiplatform UI; there is no SwiftUI or UIKit screen. Present it from anywhere on the main thread:

```swift
Peekniko.shared.present(from: nil) // top-most view controller of the active window
```

or embed `Peekniko.shared.viewController(onClose:)` yourself. From Kotlin, the same entry points are `Peekniko.present()` and `Peekniko.viewController { }`, and Compose hosts can call `Peekniko.Screen()` directly.

## Notifications

Peekniko keeps one quiet local notification listing the latest calls. It never asks for permission while capturing: nothing is posted until notifications are authorized, either by your app or by the prompt Peekniko shows the first time its screen opens (only while the status is still undetermined).

iOS hides notifications posted while the app is in the foreground unless a `UNUserNotificationCenterDelegate` opts in, so Peekniko installs its own delegate when the app has none. If your app sets its own delegate, forward to Peekniko from it:

```swift
func userNotificationCenter(_ center: UNUserNotificationCenter, willPresent notification: UNNotification) async -> UNNotificationPresentationOptions {
    Peekniko.shared.isPeeknikoNotification(notification: notification) ? [.list] : []
}

func userNotificationCenter(_ center: UNUserNotificationCenter, didReceive response: UNNotificationResponse) async {
    _ = Peekniko.shared.handleNotificationResponse(response: response)
}
```

## URLSession capture details

Headers and captured bodies may contain credentials or personal data, so enable this only in development or controlled diagnostic builds.

The adapter is written in Kotlin (`library/src/iosMain/kotlin/peekniko/internal/urlsession`); the library contains no Swift.

From the repository root, the simulator framework can also be built directly with:

```sh
./gradlew :app:linkDebugFrameworkIosSimulatorArm64
```
