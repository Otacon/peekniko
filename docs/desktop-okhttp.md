# Desktop (JVM) — OkHttp

Connect Peekniko to your OkHttp client by adding its interceptor.

## Gradle dependencies

```kotlin title="build.gradle.kts"
dependencies {
    // Debug build
    implementation("io.github.otacon.peekniko:client-okhttp:<peekniko-version>")
    implementation("com.squareup.okhttp3:okhttp:<okhttp-version>")
}
```

Keep your Peekniko dependencies on the same version and use your app's OkHttp version.

## Add the interceptor

```kotlin
import peekniko.okhttp.interceptor

val client = OkHttpClient.Builder()
    .addInterceptor(Peekniko.interceptor)
    .build()
```

Use the regular adapter for builds where you want to capture traffic, and the no-op adapter for builds where you don't.
