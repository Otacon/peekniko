package peekniko.okhttp

import peekniko.NetworkHeader
import peekniko.NetworkMethod
import peekniko.NetworkRequest
import peekniko.NetworkRepository
import peekniko.NetworkResponse
import peekniko.Peekniko
import okhttp3.Headers
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import okio.Buffer
import okio.BufferedSink
import okio.ForwardingSink
import okio.buffer
import java.util.UUID
import kotlin.time.Clock


public val Peekniko.interceptor: Interceptor by lazy { PeeknikoInterceptor(Peekniko.networkRepository) }

internal class PeeknikoInterceptor(
    private val repository: NetworkRepository,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val id = UUID.randomUUID().toString()
        val request = chain.request()
        val knownMethod = NetworkMethod.entries.firstOrNull { it.name == request.method }
        val networkRequest = NetworkRequest(
            scheme = request.url.scheme,
            host = request.url.host,
            port = request.url.port.takeUnless { it == if (request.url.isHttps) 443 else 80 },
            endpoint = request.url.encodedPath.ifEmpty { "/" },
            queryParams = request.url.encodedQuery.orEmpty(),
            method = knownMethod ?: NetworkMethod.UNKNOWN,
            headers = request.requestHeaders(),
            timestamp = Clock.System.now(),
            body = null
        )
        repository.insert(id, networkRequest)
        val trackedRequest = request.captureBody { bytes ->
            repository.insert(id, networkRequest.copy(body = bytes))
        }

        try {
            val response = chain.proceed(trackedRequest)
            val responseBytes = response.peekBody(Long.MAX_VALUE).bytes()

            val responseTimestamp = Clock.System.now()
            val responseHeaders = response.headers.toNetworkHeaders()
            val responseCode = response.code
            val tlsVersion: String?
            val cipherSuite: String?
            val handshake = response.handshake
            if (handshake != null) {
                tlsVersion = handshake.tlsVersion.javaName
                cipherSuite = handshake.cipherSuite.javaName
            } else {
                tlsVersion = null
                cipherSuite = null
            }
            repository.insert(
                id = id,
                response = NetworkResponse.Response(
                    body = responseBytes,
                    headers = responseHeaders,
                    code = responseCode,
                    timestamp = responseTimestamp,
                    protocol = response.protocol.toString(),
                    tlsVersion = tlsVersion,
                    cipherSuite = cipherSuite,
                ),
            )

            return response
        } catch (exception: Exception) {
            val message = exception.message ?: exception.toString()
            val stacktrace = exception.stackTraceToString()
            repository.insert(
                id = id,
                response = NetworkResponse.Error(
                    exceptionMessage = message,
                    stackTrace = stacktrace,
                    timestamp = Clock.System.now(),
                ),
            )
            throw exception
        }
    }

    private fun Headers.toNetworkHeaders(): List<NetworkHeader> = map { (name, value) -> NetworkHeader(name, value) }

    private fun Request.requestHeaders(): List<NetworkHeader> {
        val headers = headers.toNetworkHeaders()
        val contentType = body?.contentType() ?: return headers
        if (headers.any { it.name.equals("Content-Type", ignoreCase = true) }) return headers
        return headers + NetworkHeader("Content-Type", contentType.toString())
    }

    private fun Request.captureBody(onBodyCaptured: (ByteArray) -> Unit): Request {
        val originalBody = body ?: return this
        return newBuilder()
            .method(method, TeeRequestBody(originalBody, onBodyCaptured))
            .build()
    }
}

private class TeeRequestBody(
    private val delegate: RequestBody,
    private val onBodyCaptured: (ByteArray) -> Unit,
) : RequestBody() {
    override fun contentType() = delegate.contentType()

    override fun contentLength(): Long = delegate.contentLength()

    override fun isOneShot(): Boolean = delegate.isOneShot()

    override fun isDuplex(): Boolean = delegate.isDuplex()

    override fun writeTo(sink: BufferedSink) {
        val captured = Buffer()
        val forwardingSink = object : ForwardingSink(sink) {
            override fun write(source: Buffer, byteCount: Long) {
                source.copyTo(captured, 0L, byteCount)
                super.write(source, byteCount)
            }
        }.buffer()

        try {
            delegate.writeTo(forwardingSink)
            forwardingSink.flush()
        } finally {
            onBodyCaptured(captured.readByteArray())
        }
    }
}
