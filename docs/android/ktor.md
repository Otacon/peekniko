# Ktor

Connect Peekniko to your Ktor client by installing its plugin.

## Gradle dependencies

```kotlin title="build.gradle.kts"
dependencies {
    debugImplementation("io.github.otacon.peekniko:client-ktor:<peekniko-version>")
    releaseImplementation("io.github.otacon.peekniko:client-noop:<peekniko-version>")
    implementation("io.ktor:ktor-client-core:<ktor-version>")
}
```

Keep your Peekniko dependencies on the same version, and do the same for your Ktor dependencies.
Keep your app's existing Ktor engine dependency too.

## Install the Ktor plugin

```kotlin
import peekniko.ktor.plugin
import io.ktor.client.HttpClient

val client = HttpClient {
    install(Peekniko.plugin)
}
```

With this setup, Peekniko captures requests and responses in debug builds and skips them in release builds.
