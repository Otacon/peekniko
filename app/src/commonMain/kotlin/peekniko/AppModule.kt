package peekniko

import peekniko.playground.PlaygroundHttpClient

object AppModule {
    internal var mockServer: MockServer? = null
    internal var okhttp: PlaygroundHttpClient? = null
    internal var ktor: PlaygroundHttpClient? = null
    internal var urlSession: PlaygroundHttpClient? = null
}
