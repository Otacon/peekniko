package peekniko.internal.urlsession

import dev.mokkery.MockMode
import dev.mokkery.answering.calls
import dev.mokkery.every
import dev.mokkery.matcher.any
import dev.mokkery.mock
import dev.mokkery.verify
import dev.mokkery.verify.VerifyMode
import io.kotest.core.spec.style.FunSpec
import kotlinx.cinterop.BetaInteropApi
import kotlinx.coroutines.CompletableDeferred
import peekniko.NetworkCall
import peekniko.NetworkHeader
import peekniko.NetworkRepository
import peekniko.NetworkRequest
import peekniko.NetworkResponse
import peekniko.contract.RequestCase
import peekniko.contract.RequestRecorder
import peekniko.contract.clientRequestCases
import peekniko.contract.verifyClientRequestContract
import platform.Foundation.HTTPBody
import platform.Foundation.HTTPMethod
import platform.Foundation.NSData
import platform.Foundation.NSError
import platform.Foundation.NSHTTPURLResponse
import platform.Foundation.NSLocalizedDescriptionKey
import platform.Foundation.NSMutableURLRequest
import platform.Foundation.NSString
import platform.Foundation.NSURL
import platform.Foundation.NSURLRequest
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Foundation.dataUsingEncoding
import platform.Foundation.setValue

internal class UrlSessionInterceptorTest : FunSpec({
    context("shared client contract") {
        val recorder = RequestRecorder { case ->
            var recordedRequest: NetworkRequest? = null
            var recordedResponse: NetworkResponse? = null
            val repository = mock<NetworkRepository>()
            every { repository.insert(any(), any<NetworkRequest>()) } calls { (_: String, request: NetworkRequest) ->
                recordedRequest = request
                CompletableDeferred(true)
            }
            every { repository.insert(any(), any<NetworkResponse>()) } calls { (_: String, response: NetworkResponse) ->
                recordedResponse = response
                CompletableDeferred(true)
            }
            val listener = mock<UrlSessionListener>(MockMode.autoUnit)

            UrlSessionInterceptor(repository, cannedTransport(case)).load(case.toUrlRequest(), listener)

            if (case.responseError == null) {
                verify { listener.onFinished() }
                verify(VerifyMode.not) { listener.onError(any()) }
            } else {
                verify { listener.onError(any()) }
                verify(VerifyMode.not) { listener.onFinished() }
            }
            NetworkCall("contract", recordedRequest!!, recordedResponse)
        }

        clientRequestCases().forEach { case ->
            test(case.url) {
                verifyClientRequestContract(
                    recorder = recorder,
                    case = case,
                    expectedRequestHeaders = case.requestHeaders.asFoundationHeaders(),
                    expectedResponseHeaders = case.responseHeaders.asFoundationHeaders(),
                )
            }
        }
    }
})

private fun List<NetworkHeader>.merged(): Map<Any?, String> =
    this.groupBy({ it.name }, { it.value })
        .mapValues { (_, values) -> values.joinToString(",") }

private fun List<NetworkHeader>.asFoundationHeaders(): List<NetworkHeader> =
    merged().map { (name, value) -> NetworkHeader(name.toString(), value) }.sortedBy { it.name.lowercase() }

private fun RequestCase.toUrlRequest(): NSURLRequest =
    NSMutableURLRequest.requestWithURL(NSURL.URLWithString(url)!!).apply {
        HTTPMethod = method.name
        requestHeaders.merged().forEach { (name, value) -> setValue(value, forHTTPHeaderField = name.toString()) }
        HTTPBody = requestBody?.toNSData()
    }

private fun cannedTransport(case: RequestCase) = UrlSessionTransport { request, completion ->
    val message = case.responseError
    if (message != null) {
        val error = NSError.errorWithDomain(
            domain = "peekniko.contract",
            code = 1,
            userInfo = mapOf<Any?, Any?>(NSLocalizedDescriptionKey to message),
        )
        completion(null, null, error)
    } else {
        val response = NSHTTPURLResponse(
            uRL = request.URL!!,
            statusCode = case.responseCode.toLong(),
            HTTPVersion = "HTTP/1.1",
            headerFields = case.responseHeaders.merged(),
        )
        completion(case.responseBody.toNSData(), response, null)
    }
    return@UrlSessionTransport {}
}

@OptIn(BetaInteropApi::class)
private fun String.toNSData(): NSData = NSString.create(string = this).dataUsingEncoding(NSUTF8StringEncoding)!!
