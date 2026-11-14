package peekniko.playground

import kotlinx.cinterop.BetaInteropApi
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Foundation.NSHTTPURLResponse
import platform.Foundation.NSMutableURLRequest
import platform.Foundation.NSString
import platform.Foundation.NSURL
import platform.Foundation.NSURLSession
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.HTTPBody
import platform.Foundation.HTTPMethod
import platform.Foundation.create
import platform.Foundation.dataUsingEncoding
import platform.Foundation.dataTaskWithRequest
import platform.Foundation.setValue
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@OptIn(BetaInteropApi::class)
internal class IosUrlSessionPlaygroundClient : PlaygroundHttpClient {
    override suspend fun execute(baseUrl: String, request: PlaygroundRequest): PlaygroundResponse =
        suspendCancellableCoroutine { continuation ->
            val url = NSURL.URLWithString(baseUrl.removeSuffix("/") + request.path)
            if (url == null) {
                continuation.resumeWithException(IllegalArgumentException("Invalid URL"))
                return@suspendCancellableCoroutine
            }

            val nativeRequest = NSMutableURLRequest.requestWithURL(url)
            nativeRequest.HTTPMethod = request.method.name
            request.headers.forEach { header ->
                nativeRequest.setValue(header.value, forHTTPHeaderField = header.name)
            }
            request.body?.let { body ->
                nativeRequest.HTTPBody = NSString.create(string = body)
                    .dataUsingEncoding(NSUTF8StringEncoding)
            }

            val task = NSURLSession.sharedSession.dataTaskWithRequest(
                request = nativeRequest,
                completionHandler = { data, response, error ->
                    if (!continuation.isActive) return@dataTaskWithRequest
                    if (error != null) {
                        continuation.resumeWithException(IllegalStateException(error.localizedDescription))
                        return@dataTaskWithRequest
                    }

                    val httpResponse = response as? NSHTTPURLResponse
                    val responseBody = data?.let {
                        NSString.create(data = it, encoding = NSUTF8StringEncoding)?.toString()
                    }.orEmpty()
                    val responseHeaders = httpResponse?.allHeaderFields?.map { (name, value) ->
                        PlaygroundHeader(name = name.toString(), value = value.toString())
                    }.orEmpty()

                    continuation.resume(
                        PlaygroundResponse(
                            statusCode = httpResponse?.statusCode?.toInt() ?: 0,
                            headers = responseHeaders,
                            body = responseBody,
                        )
                    )
                },
            )
            continuation.invokeOnCancellation { task.cancel() }
            task.resume()
        }

    override fun close() = Unit
}
