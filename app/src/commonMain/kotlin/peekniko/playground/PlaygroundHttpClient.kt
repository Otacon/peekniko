package peekniko.playground

internal interface PlaygroundHttpClient : AutoCloseable {

    suspend fun execute(baseUrl: String, request: PlaygroundRequest): PlaygroundResponse
}

internal data class PlaygroundResponse(
    val statusCode: Int,
    val headers: List<PlaygroundHeader>,
    val body: String,
)

internal data class PlaygroundRequest(
    val method: PlaygroundMethod,
    val path: String,
    val headers: List<PlaygroundHeader> = emptyList(),
    val body: String? = null,
)

data class PlaygroundHeader(
    val name: String,
    val value: String,
)

enum class PlaygroundMethod {
    GET,
    HEAD,
    POST,
    PUT,
    PATCH,
    DELETE,
    OPTIONS,
    TRACE,
    CONNECT,
}
