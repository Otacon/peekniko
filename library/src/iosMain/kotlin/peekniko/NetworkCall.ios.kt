package peekniko

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.posix.memcpy
import kotlin.time.Instant

public fun NetworkRequest.Companion.create(
    scheme: String,
    host: String,
    port: Int?,
    endpoint: String,
    queryParams: String,
    method: String,
    headers: List<NetworkHeader>,
    body: NSData?,
    requestTimestampEpochMillis: Long,
): NetworkRequest = NetworkRequest(
    scheme = scheme,
    host = host,
    port = port,
    endpoint = endpoint,
    queryParams = queryParams,
    method = NetworkMethod.entries.firstOrNull { it.name == method.uppercase() }
        ?: NetworkMethod.UNKNOWN,
    body = body?.toKotlinByteArray(),
    headers = headers,
    timestamp = Instant.fromEpochMilliseconds(requestTimestampEpochMillis),
)

public fun NetworkResponse.Response.Companion.create(
    body: NSData?,
    headers: List<NetworkHeader>,
    code: Int,
    responseTimestampEpochMillis: Long,
): NetworkResponse.Response = NetworkResponse.Response(
    body = body?.toKotlinByteArray(),
    headers = headers,
    code = code,
    timestamp = Instant.fromEpochMilliseconds(responseTimestampEpochMillis),
)

public fun NetworkResponse.Error.Companion.create(
    exceptionMessage: String,
    stackTrace: String,
    responseTimestampEpochMillis: Long,
): NetworkResponse.Error = NetworkResponse.Error(
    exceptionMessage = exceptionMessage,
    stackTrace = stackTrace,
    timestamp = Instant.fromEpochMilliseconds(responseTimestampEpochMillis),
)

@OptIn(ExperimentalForeignApi::class)
internal fun NSData.toKotlinByteArray(): ByteArray? {
    if (length == 0UL) return null
    val source = bytes ?: return null
    return ByteArray(length.toInt()).also { result ->
        result.usePinned { destination -> memcpy(destination.addressOf(0), source, length) }
    }
}
