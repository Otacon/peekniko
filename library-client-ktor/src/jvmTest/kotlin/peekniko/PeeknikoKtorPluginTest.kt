package peekniko

import peekniko.contract.RequestRecorder
import peekniko.contract.clientRequestCases
import peekniko.contract.verifyClientRequestContract
import peekniko.NetworkHeader
import peekniko.ktor.createPeeknikoKtorPlugin
import dev.mokkery.answering.calls
import dev.mokkery.every
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.mock
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.headersOf
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import peekniko.contract.RequestCase

internal class PeeknikoKtorPluginTest : FunSpec({
    lateinit var repository: NetworkRepository
    lateinit var recordedRequest: NetworkRequest
    lateinit var recordedResponse: NetworkResponse

    beforeSpec {
        repository = mock()
        every { repository.insert(any(), any<NetworkRequest>()) } calls { (_: String, request: NetworkRequest) ->
            recordedRequest = request
            CompletableDeferred(true)
        }
        every { repository.insert(any(), any<NetworkResponse>()) } calls { (_: String, response: NetworkResponse) ->
            recordedResponse = response
            CompletableDeferred(true)
        }
        everySuspend { repository.getById(any()) } calls { (id: String) -> NetworkCall(id, recordedRequest) }
        every { repository.observeCallUpdates(any()) } calls { (_: Int) -> MutableStateFlow(emptyList()) }
    }

    test("Ktor sends a request and exposes a successful response") {
        val responseBody = "response payload"
        val client = HttpClient(MockEngine {
            respond(
                content = ByteReadChannel(responseBody),
                status = HttpStatusCode.Created,
                headers = headersOf("X-Scenario", "created"),
            )
        }) {
            expectSuccess = false
            install(createPeeknikoKtorPlugin(repository))
        }
        try {
            client.post("http://localhost:8080/items/42?view=full&sort=asc") {
                header("X-Label", "first")
                header("X-Label", "second")
                contentType(ContentType.parse("application/json; charset=utf-8"))
                setBody("request payload")
            }.bodyAsText() shouldBe responseBody
        } finally {
            client.close()
        }
    }

    test("Ktor exposes an HTTP error response body") {
        val responseBody = "request failed"
        val client = HttpClient(MockEngine {
            respond(content = ByteReadChannel(responseBody), status = HttpStatusCode.InternalServerError)
        }) {
            expectSuccess = false
            install(createPeeknikoKtorPlugin(repository))
        }
        try {
            client.request("http://localhost:8080/server-error").bodyAsText() shouldBe responseBody
        } finally {
            client.close()
        }
    }

    context("shared client contract") {
        val recorder = RequestRecorder { case ->
            val client = HttpClient(MockEngine {
                case.responseError?.let { throw IllegalStateException(it) }
                respond(
                    content = ByteReadChannel(case.responseBody),
                    status = HttpStatusCode.fromValue(case.responseCode),
                    headers = headersOf(
                        *case.responseHeaders.groupBy { it.name }
                            .map { (name, values) -> name to values.map { it.value } }
                            .toTypedArray(),
                    ),
                )
            }) {
                expectSuccess = false
                install(createPeeknikoKtorPlugin(repository))
            }
            client.use { client ->
                try {
                    client.request(case.url) {
                        method = HttpMethod.parse(case.method.name)
                        case.requestHeaders.forEach { header(it.name, it.value) }
                        case.requestBody?.let { setBody(it) }
                    }.bodyAsText()
                } catch (exception: Exception) {
                    if (case.responseError == null) throw exception
                }
                NetworkCall("contract", recordedRequest, recordedResponse)
            }
        }
        clientRequestCases().forEach { case ->
            test(case.url) {
                verifyClientRequestContract(recorder, case, expectedKtorRequestHeaders(case))
            }
        }
    }
})

private fun expectedKtorRequestHeaders(case: RequestCase): List<NetworkHeader> = buildList {
    addAll(case.requestHeaders.filterNot { it.name.equals("Content-Type", ignoreCase = true) })
    add(NetworkHeader("Accept", "*/*"))
    if (case.requestBody != null) {
        add(
            case.requestHeaders.firstOrNull { it.name.equals("Content-Type", ignoreCase = true) }
                ?: NetworkHeader("Content-Type", "text/plain; charset=UTF-8"),
        )
    }
    val contentLength = case.requestBody?.encodeToByteArray()?.size ?: 0
    add(NetworkHeader("Content-Length", contentLength.toString()))
}
