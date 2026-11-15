<p align="center">
  <img src="docs/assets/peekniko-logo.svg" alt="Peekniko logo" width="120" />
</p>

<h1 align="center">Peekniko</h1>

<p align="center">A lightweight network inspector for your app. Capture and explore HTTP requests and responses right on your device.</p>

<p align="center">
  <a href="https://kotlinlang.org"><img src="https://img.shields.io/badge/Kotlin-7F52FF?logo=kotlin&logoColor=white" alt="Kotlin"></a>
  <a href="https://kotlinlang.org/docs/multiplatform.html"><img src="https://img.shields.io/badge/Kotlin%20Multiplatform-supported-7F52FF?logo=kotlin&logoColor=white" alt="Kotlin Multiplatform"></a>
  <img src="https://img.shields.io/badge/platform-Android-3DDC84?logo=android&logoColor=white" alt="Android">
  <img src="https://img.shields.io/badge/Android%20API-22%2B-3DDC84?logo=android&logoColor=white" alt="Minimum Android API 22">
  <a href="https://search.maven.org/artifact/io.github.otacon.peekniko/client-okhttp"><img src="https://img.shields.io/maven-central/v/io.github.otacon.peekniko/client-okhttp?label=Maven%20Central" alt="Maven Central version"></a>
  <a href="LICENSE"><img src="https://img.shields.io/github/license/Otacon/peekniko" alt="Apache 2.0 license"></a>
  <a href="https://github.com/Otacon/peekniko"><img src="https://img.shields.io/github/stars/Otacon/peekniko?style=social" alt="GitHub stars"></a>
</p>

<p align="center">
  <img src="assets/screenshot_list.png" alt="Peekniko request list" width="30%" />
  <img src="assets/screenshot_summary.png" alt="Peekniko request summary" width="30%" />
  <img src="assets/screenshot_response.png" alt="Peekniko response details" width="30%" />
</p>

## Compatibility

| Library              | Platform                        |
|----------------------|---------------------------------|
| OkHttp (Client)      | Android, Desktop                |
| Ktor (Client)        | Android, Desktop (iOS, Web WIP) |
| URLSession (Client)  | iOS (WIP)                       |
| SQLDelight (Storage) | Android, Desktop (iOS, Web WIP) |

[Documentation](https://otacon.github.io/peekniko/)

## Quick start: OkHttp + SQLDelight

Ensure Maven Central is included in `settings.gradle.kts`:

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}
```

Add the dependencies to your app module's `build.gradle.kts` (replace the version placeholders):

```kotlin
dependencies {
    debugImplementation("io.github.otacon.peekniko:client-okhttp:<peekniko-version>")
    debugImplementation("io.github.otacon.peekniko:storage-sqldelight:<peekniko-version>")
    releaseImplementation("io.github.otacon.peekniko:client-noop:<peekniko-version>")
}
```

If your app already uses SQLDelight, you don't need to add any SQLDelight dependencies. Otherwise, add them with
`debugImplementation`:

```kotlin
dependencies {
    debugImplementation("app.cash.sqldelight:runtime:<sqldelight-version>")
    debugImplementation("app.cash.sqldelight:coroutines-extensions:<sqldelight-version>")
    debugImplementation("app.cash.sqldelight:android-driver:<sqldelight-version>")
}
```

Configure persistent storage before building your OkHttp client:

```kotlin
import peekniko.Peekniko
import peekniko.storage.sqldelight.sqlDelightStorage
import peekniko.okhttp.interceptor

Peekniko.configure {
    sqlDelightStorage(name = "peekniko.db")
}

val client = OkHttpClient.Builder()
    .addInterceptor(Peekniko.interceptor)
    .build()
```
