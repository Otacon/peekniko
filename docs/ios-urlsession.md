# iOS — URLSession

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

In a Swift-only app, build the framework and add the `library` folder to Xcode as a local package, then `import PeeknikoCore`:

```sh
./gradlew :library:preparePeeknikoKitXCFramework
```

## Register the protocol

Configure Peekniko and register the protocol once at startup, before creating requests:

```swift
Peekniko.shared.configure { configuration in
    configuration.inMemoryStorage(maxApiCalls: 100)
}
Peekniko.shared.registerUrlProtocol()
```

Sessions created with their own `URLSessionConfiguration` need the protocol added explicitly:

```swift
let configuration = URLSessionConfiguration.default
Peekniko.shared.addUrlProtocol(configuration: configuration)
```

From Kotlin, the same functions are `Peekniko.registerUrlProtocol()` and `Peekniko.addUrlProtocol(configuration)`.

## Open Peekniko

```swift
Peekniko.shared.present(from: nil)
```

See [iOS — Ktor](ios-ktor.md#notifications) for notification setup.
