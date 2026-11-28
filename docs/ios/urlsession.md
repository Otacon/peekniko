# URLSession

Capture `URLSession` traffic by registering Peekniko's `URLProtocol`.

## Setup

With the Swift package, no setup is needed.

In a Kotlin Multiplatform app, add the dependency to your shared module and export it from your framework:

```kotlin title="build.gradle.kts"
kotlin {
    sourceSets {
        iosMain.dependencies {
            api("io.github.otacon.peekniko:client-core:<peekniko-version>")
        }
    }
}
```

## Register the protocol

Register the protocol once at startup, after configuring Peekniko and before creating requests:

```swift
import Peekniko

Peekniko.shared.registerUrlProtocol()
```

Sessions created with their own `URLSessionConfiguration` need the protocol added explicitly:

```swift
let configuration = URLSessionConfiguration.default
Peekniko.shared.addUrlProtocol(configuration: configuration)
```

From Kotlin, the same functions are `Peekniko.registerUrlProtocol()` and `Peekniko.addUrlProtocol(configuration)`.
