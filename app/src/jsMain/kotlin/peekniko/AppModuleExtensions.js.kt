package peekniko

import peekniko.ktor.plugin
import peekniko.playground.KtorPlaygroundClient
import io.ktor.client.HttpClient
import io.ktor.client.engine.js.Js

fun AppModule.initialize() {
    Peekniko.configure { inMemoryStorage() }
    mockServer = null
    okhttp = null
    urlSession = null
    val ktorClient = HttpClient(Js) {
        expectSuccess = false
        install(Peekniko.plugin)
    }
    ktor = KtorPlaygroundClient(ktorClient)
}
