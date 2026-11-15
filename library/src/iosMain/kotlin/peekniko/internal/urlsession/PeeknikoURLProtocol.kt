@file:OptIn(BetaInteropApi::class, ExperimentalForeignApi::class)

package peekniko.internal.urlsession

import kotlinx.cinterop.*
import peekniko.Peekniko
import platform.Foundation.*

private const val HANDLED_KEY = "PeeknikoURLProtocolHandledKey"

private val sharedSessionTransport = UrlSessionTransport { request, completion ->
    val task = NSURLSession.sharedSession.dataTaskWithRequest(request, completionHandler = completion)
    task.resume()
    task::cancel
}

private val interceptor by lazy {
    UrlSessionInterceptor(Peekniko.networkRepository, sharedSessionTransport)
}

internal class PeeknikoURLProtocol @OverrideInit constructor(
    request: NSURLRequest,
    cachedResponse: NSCachedURLResponse?,
    client: NSURLProtocolClientProtocol?,
) : NSURLProtocol(request = request, cachedResponse = cachedResponse, client = client) {
    private var cancel: (() -> Unit)? = null

    override fun startLoading() {
        val forwarded = request.mutableCopy() as NSMutableURLRequest
        NSURLProtocol.setProperty(true, forKey = HANDLED_KEY, inRequest = forwarded)
        if (forwarded.HTTPBody == null) {
            forwarded.HTTPBody = request.HTTPBodyStream?.readAll()
        }
        cancel = interceptor.load(forwarded, ClientListener(this))
    }

    override fun stopLoading() {
        cancel?.invoke()
        cancel = null
    }

    companion object : NSURLProtocolMeta() {
        override fun canInitWithRequest(request: NSURLRequest): Boolean {
            val scheme = request.URL?.scheme?.lowercase()
            return (scheme == "http" || scheme == "https") &&
                    NSURLProtocol.propertyForKey(HANDLED_KEY, inRequest = request) == null
        }

        override fun canonicalRequestForRequest(request: NSURLRequest): NSURLRequest = request
    }
}

private class ClientListener(private val protocol: NSURLProtocol) : UrlSessionListener {
    override fun onResponse(response: NSURLResponse) {
        protocol.client?.URLProtocol(
            protocol,
            didReceiveResponse = response,
            cacheStoragePolicy = NSURLCacheStoragePolicy.NSURLCacheStorageNotAllowed,
        )
    }

    override fun onData(data: NSData) {
        protocol.client?.URLProtocol(protocol, didLoadData = data)
    }

    override fun onFinished() {
        protocol.client?.URLProtocolDidFinishLoading(protocol)
    }

    override fun onError(error: NSError) {
        protocol.client?.URLProtocol(protocol, didFailWithError = error)
    }
}

private fun NSInputStream.readAll(): NSData? {
    val data = NSMutableData()
    val buffer = ByteArray(4096)
    open()
    try {
        buffer.usePinned { pinned ->
            while (hasBytesAvailable) {
                val read = read(pinned.addressOf(0).reinterpret(), buffer.size.convert())
                if (read <= 0) break
                data.appendBytes(pinned.addressOf(0), read.convert())
            }
        }
    } finally {
        close()
    }
    return data.takeIf { it.length > 0u }
}
