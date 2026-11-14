package peekniko

import kotlin.time.Instant

internal fun sampleRequest(
    timestamp: Long = 0,
    scheme: String = "https",
    host: String = "example.com",
    port: Int? = null,
    endpoint: String = "/$timestamp",
    queryParams: String = "",
    method: NetworkMethod = NetworkMethod.GET,
    body: ByteArray? = null,
    headers: List<NetworkHeader> = listOf(NetworkHeader("Request", "header")),
) = NetworkRequest(
    scheme = scheme,
    host = host,
    port = port,
    endpoint = endpoint,
    queryParams = queryParams,
    method = method,
    body = body,
    headers = headers,
    timestamp = Instant.fromEpochMilliseconds(timestamp),
)

internal fun sampleResponse(
    body: ByteArray? = null,
    headers: List<NetworkHeader> = listOf(NetworkHeader("Response", "header")),
    code: Int = 201,
    timestamp: Long = 11,
) = NetworkResponse.Response(
    body = body,
    headers = headers,
    code = code,
    timestamp = Instant.fromEpochMilliseconds(timestamp),
)

internal fun sampleError(
    message: String = "connection failed",
    stackTrace: String = "trace line 1\ntrace line 2",
    timestamp: Long = 789,
) = NetworkResponse.Error(
    exceptionMessage = message,
    stackTrace = stackTrace,
    timestamp = Instant.fromEpochMilliseconds(timestamp),
)

internal fun sampleCall(
    id: String = "test-id",
    request: NetworkRequest = sampleRequest(),
    response: NetworkResponse? = null,
) = NetworkCall(id = id, request = request, response = response)
