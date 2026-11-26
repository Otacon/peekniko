# URLSession

Capture `URLSession` traffic by registering Peekniko's `URLProtocol`.

## Setup

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

In a Swift-only app, add the `Peekniko` product with [Swift Package Manager](index.md#swift-package-manager), then
`import Peekniko`.

To try local changes, build the framework and add the `library-ios` folder to Xcode as a local package:

```sh
./gradlew :library-ios:assemblePeeknikoCoreDebugXCFramework
```

## Register the protocol

Register the protocol once at startup, after configuring Peekniko and before creating requests:

```swift
Peekniko.shared.registerUrlProtocol()
```

Sessions created with their own `URLSessionConfiguration` need the protocol added explicitly:

```swift
let configuration = URLSessionConfiguration.default
Peekniko.shared.addUrlProtocol(configuration: configuration)
```

From Kotlin, the same functions are `Peekniko.registerUrlProtocol()` and `Peekniko.addUrlProtocol(configuration)`.
