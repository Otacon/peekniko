# Desktop (JVM) — Ktor

Connect Peekniko to your Ktor client by installing its plugin.

## Gradle dependencies

```kotlin title="build.gradle.kts"
dependencies {
    // Debug build
    implementation("io.github.otacon.peekniko:client-ktor:<peekniko-version>")
    implementation("io.ktor:ktor-client-core:<ktor-version>")
    implementation("io.ktor:ktor-client-okhttp:<ktor-version>")
}
```

Keep your Peekniko dependencies on the same version, and do the same for your Ktor dependencies.
This example uses OkHttp, but you can use any supported JVM Ktor engine.

## Install the Ktor plugin

```kotlin
import peekniko.ktor.plugin
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp

val client = HttpClient(OkHttp) {
    install(Peekniko.plugin)
}
```

Use the regular adapter for builds where you want to capture traffic, and the no-op adapter for builds where you don't.
