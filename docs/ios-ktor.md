# iOS — Ktor

Connect Peekniko to your Ktor client by installing its plugin in your shared Kotlin Multiplatform module.

## Gradle dependencies

```kotlin title="build.gradle.kts"
kotlin {
    sourceSets {
        iosMain.dependencies {
            implementation("io.github.otacon.peekniko:client-ktor:<peekniko-version>")
            implementation("io.ktor:ktor-client-darwin:<ktor-version>")
        }
    }
}
```

Keep your Peekniko dependencies on the same version, and do the same for your Ktor dependencies.

## Install the Ktor plugin

```kotlin
import peekniko.Peekniko
import peekniko.ktor.plugin
import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin

Peekniko.configure { }

val client = HttpClient(Darwin) {
    install(Peekniko.plugin)
}
```

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
