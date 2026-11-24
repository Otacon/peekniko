package peekniko.ktor

import peekniko.NetworkHeader
import peekniko.NetworkMethod
import peekniko.NetworkRequest
import peekniko.NetworkRepository
import peekniko.NetworkResponse
import peekniko.Peekniko
import io.ktor.client.call.HttpClientCall
import io.ktor.client.plugins.api.ClientPlugin
import io.ktor.client.plugins.api.Send
import io.ktor.client.plugins.api.SendingRequest
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.statement.bodyAsBytes
import io.ktor.http.Headers
import io.ktor.http.content.OutgoingContent
import io.ktor.util.AttributeKey
import kotlin.random.Random
import kotlin.time.Clock

private val networkCallIdKey: AttributeKey<String> = AttributeKey("PeeknikoNetworkCallId")

public val Peekniko.plugin: ClientPlugin<*>
    get() = createPeeknikoKtorPlugin(Peekniko.networkRepository)

internal fun createPeeknikoKtorPlugin(repository: NetworkRepository): ClientPlugin<*> =
    createClientPlugin("PeeknikoKtorPlugin") {
        on(Send) { request ->
            val id = Random.nextLong().toString()
            request.networkCallId = id
            repository.insert(id, request.toNetworkRequest()).await()
            try {
                proceed(request)
            } catch (exception: Exception) {
                val message = exception.message ?: exception.toString()
                repository.insert(
                    id,
                    NetworkResponse.Error(
                        exceptionMessage = message,
                        stackTrace = exception.stackTraceToString(),
                        timestamp = Clock.System.now(),
                    ),
                ).await()
                throw exception
            }
        }

        on(SendingRequest) { request, content ->
            val id = request.networkCallId
            if (id != null) {
                val preparedHeaders = request.headers.build().toNetworkHeaders() + content.toNetworkHeaders()
                val current = repository.getById(id) ?: return@on
                val preparedBody = content.toBodyBytes() ?: current.request.body
                repository.insert(id, current.request.copy(headers = preparedHeaders, body = preparedBody)).await()
            }
        }

        onResponse { response ->
            val id = response.call.networkCallId ?: return@onResponse
            val responseBody = response.bodyAsBytes()
            repository.insert(
                id = id,
                response = NetworkResponse.Response(
                    body = responseBody,
                    headers = response.headers.toNetworkHeaders(),
                    code = response.status.value,
                    timestamp = Clock.System.now(),
                    protocol = response.version.toString(),
                    tlsVersion = null,
                    cipherSuite = null,
                ),
            ).await()
        }
    }

private var HttpRequestBuilder.networkCallId: String?
    get() = attributes.getOrNull(networkCallIdKey)
    set(value) {
        if (value != null) attributes.put(networkCallIdKey, value)
    }

private val HttpClientCall.networkCallId: String?
    get() = attributes.getOrNull(networkCallIdKey)

private fun HttpRequestBuilder.toNetworkRequest(): NetworkRequest {
    val parsedUrl = url.build()
    return NetworkRequest(
        scheme = parsedUrl.protocol.name,
        host = parsedUrl.host,
        port = parsedUrl.port.takeUnless { it == parsedUrl.protocol.defaultPort },
        endpoint = parsedUrl.encodedPath.ifEmpty { "/" },
        queryParams = parsedUrl.encodedQuery,
        method = NetworkMethod.entries.firstOrNull { it.name == method.value } ?: NetworkMethod.UNKNOWN,
        body = body.toBodyBytes(),
        headers = headers.build().toNetworkHeaders(),
        timestamp = Clock.System.now(),
    )
}

private fun Any?.toBodyBytes(): ByteArray? = when (this) {
    is String -> encodeToByteArray()
    is ByteArray -> copyOf()
    is OutgoingContent.ByteArrayContent -> bytes()
    else -> null
}

private fun Headers.toNetworkHeaders(): List<NetworkHeader> =
    entries().flatMap { (name, values) -> values.map { value -> NetworkHeader(name, value) } }

private fun OutgoingContent.toNetworkHeaders(): List<NetworkHeader> = buildList {
    addAll(headers.toNetworkHeaders())
    contentType?.let { add(NetworkHeader("Content-Type", it.toString())) }
    contentLength?.let { add(NetworkHeader("Content-Length", it.toString())) }
}.distinctBy { it.name.lowercase() to it.value }
