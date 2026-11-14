# Android — OkHttp

Connect Peekniko to your OkHttp client by adding its interceptor.

## Gradle dependencies

```kotlin title="build.gradle.kts"
dependencies {
    debugImplementation("io.github.otacon.peekniko:client-okhttp:<peekniko-version>")
    releaseImplementation("io.github.otacon.peekniko:client-noop:<peekniko-version>")
    implementation("com.squareup.okhttp3:okhttp:<okhttp-version>")
}
```

Keep your Peekniko dependencies on the same version and use your app's OkHttp version.
If you use custom build variants, choose which ones should capture traffic.

## Add the interceptor

```kotlin
import peekniko.okhttp.interceptor

val client = OkHttpClient.Builder()
    .addInterceptor(Peekniko.interceptor)
    .build()
```

With this setup, Peekniko captures requests and responses in debug builds and skips them in release builds.
