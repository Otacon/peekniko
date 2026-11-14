# iOS app

Open `PeeknikoApp.xcodeproj` in Xcode, select an iOS 17+ simulator or device, and run the `PeeknikoApp` scheme. The Xcode build phase builds the matching Kotlin `App.framework` from the repository's `app` module, and the SwiftUI host presents its shared Compose screen.

The shared iOS app target uses Ktor's Darwin engine. The OkHttp action is only shown on Android/JVM.

The sample app registers global `URLSession` inspection in `PeeknikoApp.init`. Tap **Run native URLSession request**, then open Peekniko to inspect it. In another host app, register once during startup, before creating requests:

```swift
Peekniko.shared.configure { configuration in
    configuration.inMemoryStorage(maxApiCalls: 100)
}
URLProtocol.registerClass(Peekniko.shared.urlProtocol)
```

`URLProtocol.unregisterClass(Peekniko.shared.urlProtocol)` disables capture. Global registration makes the protocol available to the URL loading system, but Apple doesn’t guarantee every registered protocol will be consulted. Background sessions and networking stacks that bypass `URLProtocol` aren’t captured. The protocol forwards requests through its own default session to avoid recursively intercepting itself, so custom per-session proxy, cache, cookie, or TLS trust settings aren’t carried over.

Request and response bodies are captured only when they are UTF-8 and no larger than 1 MiB. Streamed request bodies bypass inspection, and larger bodies are not captured. Headers and captured bodies may contain credentials or personal data, so enable this only in development or controlled diagnostic builds.
The adapter source lives at `library/src/iosMain/swift/PeeknikoURLProtocol.swift` and is included by the local Swift package.

From the repository root, the simulator framework can also be built directly with:

```sh
./gradlew :app:linkDebugFrameworkIosSimulatorArm64
```
