# Peekniko

Peekniko helps you inspect network requests and responses in your Kotlin Multiplatform app.
Choose a client integration below to get started.

## Basic configuration

Configure Peekniko once when your app starts, before creating your HTTP client:

```kotlin
import peekniko.Peekniko

Peekniko.configure {
    // Add any storage options here, or leave empty to use the defaults.
}
```

By default, Peekniko keeps captures in memory. To keep them between app launches, choose a storage option below.

## Clients

### OkHttp

- [Android](android-okhttp.md)
- [Desktop (JVM)](desktop-okhttp.md)

### Ktor

- [iOS](ios-ktor.md)
- [Android](android-ktor.md)
- [Desktop (JVM)](desktop-ktor.md)

### Native URL loading

- [iOS — URLSession](ios-urlsession.md)

## Storage

Want to keep your captures between app launches? Add SQLDelight storage for your platform:

- [iOS](storage-ios.md)
- [Android](storage-android.md)
- [Desktop (JVM)](storage-desktop.md)
