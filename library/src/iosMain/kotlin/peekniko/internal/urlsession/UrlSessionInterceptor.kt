package peekniko.internal.urlsession

import peekniko.*
import platform.Foundation.*
import kotlin.time.Clock

internal fun interface UrlSessionTransport {
    fun execute(
        request: NSURLRequest,
        completion: (data: NSData?, response: NSURLResponse?, error: NSError?) -> Unit,
    ): () -> Unit
}

internal interface UrlSessionListener {
    fun onResponse(response: NSURLResponse)
    fun onData(data: NSData)
    fun onFinished()
    fun onError(error: NSError)
}

internal class UrlSessionInterceptor(
    private val repository: NetworkRepository,
    private val transport: UrlSessionTransport,
) {
    fun load(request: NSURLRequest, listener: UrlSessionListener): () -> Unit {
        val id = NSUUID().UUIDString
        repository.insert(id, request.toNetworkRequest())
        return transport.execute(request) { data, response, error ->
            if (error != null) {
                repository.insert(id, error.toNetworkError())
                listener.onError(error)
                return@execute
            }
            if (response is NSHTTPURLResponse) {
                repository.insert(id, response.toNetworkResponse(data))
            }
            response?.let(listener::onResponse)
            data?.let(listener::onData)
            listener.onFinished()
        }
    }
}

private fun NSURLRequest.toNetworkRequest(): NetworkRequest {
    val components = URL?.let { NSURLComponents.componentsWithURL(it, resolvingAgainstBaseURL = false) }
    val scheme = components?.scheme.orEmpty()
    val defaultPort = when (scheme.lowercase()) {
        "http" -> 80
        "https" -> 443
        else -> null
    }
    return NetworkRequest(
        scheme = scheme,
        host = components?.host.orEmpty(),
        port = components?.port?.intValue?.takeUnless { it == defaultPort },
        endpoint = components?.percentEncodedPath.orEmpty().ifEmpty { "/" },
        queryParams = components?.percentEncodedQuery.orEmpty(),
        method = NetworkMethod.entries.firstOrNull { it.name == HTTPMethod?.uppercase() } ?: NetworkMethod.UNKNOWN,
        body = HTTPBody?.toKotlinByteArray(),
        headers = allHTTPHeaderFields.toNetworkHeaders(),
        timestamp = Clock.System.now(),
    )
}

private fun NSHTTPURLResponse.toNetworkResponse(data: NSData?): NetworkResponse.Response =
    NetworkResponse.Response(
        body = data?.toKotlinByteArray(),
        headers = allHeaderFields.toNetworkHeaders(),
        code = statusCode.toInt(),
        timestamp = Clock.System.now(),
    )

private fun NSError.toNetworkError(): NetworkResponse.Error =
    NetworkResponse.Error(
        exceptionMessage = localizedDescription,
        stackTrace = NSThread.callStackSymbols.joinToString("\n"),
        timestamp = Clock.System.now(),
    )

private fun Map<Any?, *>?.toNetworkHeaders(): List<NetworkHeader> = orEmpty()
    .map { (name, value) -> NetworkHeader(name.toString(), value.toString()) }
    .sortedBy { it.name.lowercase() }
