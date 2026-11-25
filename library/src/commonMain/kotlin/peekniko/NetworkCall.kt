package peekniko

import kotlin.time.Instant

public enum class NetworkMethod {
    GET,
    HEAD,
    POST,
    PUT,
    PATCH,
    DELETE,
    OPTIONS,
    TRACE,
    CONNECT,
    UNKNOWN,
}

public data class NetworkHeader(
    public val name: String,
    public val value: String,
)

public data class NetworkRequest(
    public val scheme: String,
    public val host: String,
    public val port: Int?,
    public val endpoint: String,
    public val queryParams: String,
    public val method: NetworkMethod,
    public val body: ByteArray?,
    public val headers: List<NetworkHeader> = emptyList(),
    public val timestamp: Instant,
) {
    public companion object
}

public val NetworkRequest.fullUrl: String
    get() = buildString {
        append(scheme)
        append("://")
        append(host)
        port?.let { append(":$it") }
        append(endpoint)
        if (queryParams.isNotEmpty()) {
            append("?$queryParams")
        }
    }

public val NetworkRequest.pathAndQueryParams: String
    get() = buildString {
        append(endpoint)
        if (queryParams.isNotEmpty()) {
            append("?$queryParams")
        }
    }

public val NetworkRequest.isSsl: Boolean get() = scheme.equals("https", ignoreCase = true)

public sealed interface NetworkResponse {
    public val timestamp: Instant

    public data class Response(
        public val body: ByteArray?,
        public val headers: List<NetworkHeader>,
        public val code: Int,
        public val protocol: String?,
        public val tlsVersion: String?,
        public val cipherSuite: String?,
        override val timestamp: Instant,
    ) : NetworkResponse {
        public companion object
    }

    public data class Error(
        public val exceptionMessage: String,
        public val stackTrace: String,
        override val timestamp: Instant,
    ) : NetworkResponse {
        public companion object
    }
}

public data class NetworkCall(
    public val id: String,
    public val request: NetworkRequest,
    public val response: NetworkResponse? = null,
) {
    public fun sameContent(other: NetworkCall): Boolean =
        id == other.id && request.sameContent(other.request) && response.sameContent(other.response)

    public companion object
}

private fun NetworkRequest.sameContent(other: NetworkRequest): Boolean =
    scheme == other.scheme && host == other.host && port == other.port &&
            endpoint == other.endpoint && queryParams == other.queryParams &&
            method == other.method && body?.size == other.body?.size &&
            headers == other.headers && timestamp == other.timestamp

private fun NetworkResponse?.sameContent(other: NetworkResponse?): Boolean = when {
    this === other -> true
    this == null || other == null -> false
    this is NetworkResponse.Response && other is NetworkResponse.Response ->
        body?.size == other.body?.size && headers == other.headers && code == other.code &&
                timestamp == other.timestamp

    this is NetworkResponse.Error && other is NetworkResponse.Error ->
        exceptionMessage == other.exceptionMessage && stackTrace == other.stackTrace &&
                timestamp == other.timestamp

    else -> false
}

public data class NetworkCallSummary(
    public val id: String,
    public val method: NetworkMethod,
    public val endpoint: String,
    public val queryParams: String,
    public val requestTimestamp: Instant,
    public val requestBodySize: Long?,
    public val responseCode: Int?,
    public val responseBodySize: Long?,
    public val responseTimestamp: Instant?,
    public val exceptionMessage: String?,
)

public val NetworkCallSummary.pathAndQueryParams: String
    get() = buildString {
        append(endpoint)
        if (queryParams.isNotEmpty()) {
            append("?$queryParams")
        }
    }

internal fun NetworkCall.toSummary(): NetworkCallSummary = NetworkCallSummary(
    id = id,
    method = request.method,
    endpoint = request.endpoint,
    queryParams = request.queryParams,
    requestTimestamp = request.timestamp,
    requestBodySize = request.body?.size?.toLong(),
    responseCode = (response as? NetworkResponse.Response)?.code,
    responseBodySize = (response as? NetworkResponse.Response)?.body?.size?.toLong(),
    responseTimestamp = response?.timestamp,
    exceptionMessage = (response as? NetworkResponse.Error)?.exceptionMessage,
)
