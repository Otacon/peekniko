package peekniko.playground

import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.contentType

internal class KtorPlaygroundClient(private val client: HttpClient) : PlaygroundHttpClient {
    override suspend fun execute(baseUrl: String, request: PlaygroundRequest): PlaygroundResponse {
        val response = client.request(baseUrl.removeSuffix("/") + request.path) {
            method = HttpMethod(request.method.name)
            request.headers.forEach {
                if (it.name.equals("Content-Type", ignoreCase = true)) {
                    contentType(ContentType.parse(it.value))
                } else {
                    header(it.name, it.value)
                }
            }
            request.body?.let { setBody(it) }
        }

        return PlaygroundResponse(
            statusCode = response.status.value,
            headers = response.headers.entries().flatMap { (name, values) ->
                values.map { value -> PlaygroundHeader(name, value) }
            },
            body = response.bodyAsText(),
        )
    }

    override fun close() = client.close()
}
