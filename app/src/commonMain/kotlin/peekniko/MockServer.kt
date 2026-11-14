package peekniko

interface MockServer {
    fun start()
    fun close()

    val baseUrl: String
}