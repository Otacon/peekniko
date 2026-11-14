package peekniko

import peekniko.playground.KtorPlaygroundClient
import peekniko.playground.IosUrlSessionPlaygroundClient
import peekniko.ktor.plugin
import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin

fun AppModule.initialize() {
    mockServer = null
    okhttp = null
    val ktorClient = HttpClient(Darwin) {
        expectSuccess = false
        install(Peekniko.plugin)
    }
    ktor = KtorPlaygroundClient(ktorClient)
    urlSession = IosUrlSessionPlaygroundClient()
}
