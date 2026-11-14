package peekniko

import peekniko.ktor.plugin
import peekniko.okhttp.interceptor
import peekniko.playground.KtorPlaygroundClient
import peekniko.playground.OkHttpPlaygroundClient
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import okhttp3.OkHttpClient

fun AppModule.initialize() {
    mockServer = MockApiServer()
    val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(Peekniko.interceptor)
        .build()
    okhttp = OkHttpPlaygroundClient(okHttpClient)

    val ktorClient = HttpClient(OkHttp) {
        expectSuccess = false
        install(Peekniko.plugin)
    }
    ktor = KtorPlaygroundClient(ktorClient)
    urlSession = null
}
