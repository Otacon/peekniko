package peekniko

import peekniko.okhttp.PeeknikoInterceptor
import peekniko.contract.RequestRecorder
import peekniko.contract.clientRequestCases
import peekniko.contract.verifyClientRequestContract
import dev.mokkery.answering.calls
import dev.mokkery.every
import dev.mokkery.matcher.any
import dev.mokkery.mock
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CompletableDeferred
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okhttp3.MediaType.Companion.toMediaType
import okio.Buffer
import java.io.IOException
import java.lang.reflect.Proxy

internal class PeeknikoInterceptorTest : FunSpec({
    lateinit var server: MockWebServer
    lateinit var repository: NetworkRepository
    lateinit var client: OkHttpClient
    lateinit var recordedRequest: NetworkRequest
    lateinit var recordedResponse: NetworkResponse

    beforeSpec {
        server = MockWebServer().apply { start() }
    }

    beforeTest {
        repository = mock()
        every { repository.insert(any(), any<NetworkRequest>()) } calls { (_: String, value: NetworkRequest) ->
            recordedRequest = value
            CompletableDeferred(true)
        }
        every { repository.insert(any(), any<NetworkResponse>()) } calls { (_: String, value: NetworkResponse) ->
            recordedResponse = value
            CompletableDeferred(true)
        }
        client = OkHttpClient.Builder().addInterceptor(PeeknikoInterceptor(repository)).build()
    }

    afterTest {
        client.connectionPool.evictAll()
        client.dispatcher.executorService.shutdown()
    }

    afterSpec { server.close() }

    context("successful HTTP call") {
        test("sends a request and exposes the response body") {
            val requestBody = "request payload"
            val responseBody = "response payload"
            server.enqueue(
                MockResponse.Builder().code(201)
                    .addHeader("X-Scenario", "created")
                    .body(responseBody).build()
            )
            val actualResponseBody = client.newCall(
                Request.Builder()
                    .url(server.url("/items/42?view=full&sort=asc"))
                    .header("X-Request-Id", "request-123")
                    .header("X-Client", "peekniko-tests")
                    .header("X-Feature", "network-inspection")
                    .addHeader("X-Label", "first")
                    .addHeader("X-Label", "second")
                    .post(requestBody.toRequestBody())
                    .build(),
            ).execute().use { it.body.string() }
            actualResponseBody shouldBe responseBody
        }
    }

    context("unsuccessful HTTP call") {
        test("exposes an HTTP error response body") {
            val responseBody = "request failed"
            server.enqueue(MockResponse.Builder().code(500).body(responseBody).build())
            client.newCall(Request.Builder().url(server.url("/server-error")).get().build())
                .execute().use { it.body.string() shouldBe responseBody }
        }
    }

    context("shared client request contract") {
        val recorder = RequestRecorder { case ->
            val requestBuilder = Request.Builder().url(case.url)
            case.requestHeaders.forEach { requestBuilder.addHeader(it.name, it.value) }
            val request = requestBuilder
                .method(
                    case.method.name,
                    case.requestBody?.toRequestBody("text/plain; charset=UTF-8".toMediaType()),
                )
                .build()
            val chain = Proxy.newProxyInstance(
                Interceptor.Chain::class.java.classLoader,
                arrayOf(Interceptor.Chain::class.java),
            ) { _, method, args ->
                when (method.name) {
                    "request" -> request
                    "proceed" -> {
                        val proceededRequest = args!![0] as Request
                        proceededRequest.body?.writeTo(Buffer())
                        case.responseError?.let { throw IOException(it) }
                        Response.Builder()
                            .request(proceededRequest)
                            .protocol(okhttp3.Protocol.HTTP_1_1)
                            .code(case.responseCode)
                            .message("OK")
                            .apply { case.responseHeaders.forEach { addHeader(it.name, it.value) } }
                            .body(case.responseBody.toResponseBody())
                            .build()
                    }
                    else -> null
                }
            } as Interceptor.Chain

            try {
                PeeknikoInterceptor(repository).intercept(chain)
            } catch (exception: Exception) {
                if (case.responseError == null) throw exception
            }
            NetworkCall("contract", recordedRequest, recordedResponse)
        }

        clientRequestCases().forEach { case ->
            test(case.url) { verifyClientRequestContract(recorder, case) }
        }
    }

})
