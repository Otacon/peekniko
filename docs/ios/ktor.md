# Ktor

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

val client = HttpClient(Darwin) {
    install(Peekniko.plugin)
}
```
