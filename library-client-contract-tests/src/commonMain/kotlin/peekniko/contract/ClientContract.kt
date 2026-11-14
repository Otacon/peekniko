package peekniko.contract

import io.kotest.matchers.shouldBe
import peekniko.NetworkCall
import peekniko.NetworkHeader
import peekniko.NetworkMethod
import peekniko.NetworkResponse

public fun interface RequestRecorder {
    public suspend fun record(case: RequestCase): NetworkCall
}

public fun clientRequestCases(): List<RequestCase> = listOf(
    RequestCase(
        url = "https://example.com",
        scheme = "https",
        host = "example.com",
        port = null,
        endpoint = "/",
        queryParams = ""
    ),
    RequestCase(
        url = "http://example.com/items/42/?view=full&sort=asc",
        scheme = "http",
        host = "example.com",
        port = null,
        endpoint = "/items/42/",
        queryParams = "view=full&sort=asc"
    ),
    RequestCase(
        url = "https://example.com:8443/search?q=a%20b",
        scheme = "https",
        host = "example.com",
        port = 8443,
        endpoint = "/search",
        queryParams = "q=a%20b"
    ),
    RequestCase(
        url = "http://example.com:80/explicit-default-port",
        scheme = "http",
        host = "example.com",
        port = null,
        endpoint = "/explicit-default-port",
        queryParams = ""
    ),
    RequestCase(
        url = "https://example.com:443/explicit-default-port/",
        scheme = "https",
        host = "example.com",
        port = null,
        endpoint = "/explicit-default-port/",
        queryParams = ""
    ),
    RequestCase(
        url = "https://example.com/a%2Fb/%E2%9C%93",
        scheme = "https",
        host = "example.com",
        port = null,
        endpoint = "/a%2Fb/%E2%9C%93",
        queryParams = ""
    ),
    RequestCase(
        url = "https://example.com/search?tag=one&tag=two&flag&empty=",
        scheme = "https",
        host = "example.com",
        port = null,
        endpoint = "/search",
        queryParams = "tag=one&tag=two&flag&empty="
    ),
    RequestCase(
        url = "https://example.com/path?term=a%20b&next=%2F",
        scheme = "https",
        host = "example.com",
        port = null,
        endpoint = "/path",
        queryParams = "term=a%20b&next=%2F"
    ),
    RequestCase(
        url = "https://example.com/path?",
        scheme = "https",
        host = "example.com",
        port = null,
        endpoint = "/path",
        queryParams = ""
    ),
    RequestCase(
        url = "https://example.com/path?query=kept#fragment",
        scheme = "https",
        host = "example.com",
        port = null,
        endpoint = "/path",
        queryParams = "query=kept"
    ),
    RequestCase(
        url = "https://example.com/contract",
        scheme = "https",
        host = "example.com",
        port = null,
        endpoint = "/contract",
        queryParams = "",
        requestHeaders = listOf(
            NetworkHeader("X-Contract", "request"),
            NetworkHeader("X-Repeat", "first"),
            NetworkHeader("X-Repeat", "second"),
        ),
        responseCode = 201,
        responseHeaders = listOf(
            NetworkHeader("X-Response", "first"),
            NetworkHeader("X-Response", "second"),
            NetworkHeader("X-Contract", "response"),
        ),
    ),
    RequestCase(
        url = "https://example.com/contract-body",
        scheme = "https",
        host = "example.com",
        port = null,
        endpoint = "/contract-body",
        queryParams = "",
        method = NetworkMethod.POST,
        requestBody = "request-body",
        requestHeaders = listOf(
            NetworkHeader("Content-Type", "text/plain; charset=UTF-8")
        ),
    ),
    RequestCase(
        url = "https://example.com/contract-error",
        scheme = "https",
        host = "example.com",
        port = null,
        endpoint = "/contract-error",
        queryParams = "",
        responseError = "contract-error",
    ),
    RequestCase(
        url = "https://example.com/contract-server-error",
        scheme = "https",
        host = "example.com",
        port = null,
        endpoint = "/contract-server-error",
        queryParams = "",
        responseCode = 500,
        responseBody = "server-error",
    ),
)

public suspend fun verifyClientRequestContract(
    recorder: RequestRecorder,
    case: RequestCase,
    expectedRequestHeaders: List<NetworkHeader> = case.requestHeaders,
) {
    val call = recorder.record(case)
    val request = call.request
    request.scheme shouldBe case.scheme
    request.host shouldBe case.host
    request.port shouldBe case.port
    request.endpoint shouldBe case.endpoint
    request.queryParams shouldBe case.queryParams
    request.endpoint.contains('?') shouldBe false
    request.queryParams.startsWith('?') shouldBe false
    request.queryParams.contains('#') shouldBe false
    request.endpoint.isEmpty() shouldBe false
    request.endpoint.startsWith('/') shouldBe true
    request.method shouldBe case.method
    request.body?.decodeToString() shouldBe case.requestBody
    request.headers shouldBe expectedRequestHeaders
    if (case.responseError == null) {
        (call.response is NetworkResponse.Response) shouldBe true
        val response = call.response as NetworkResponse.Response
        response.code shouldBe case.responseCode
        response.body?.decodeToString() shouldBe case.responseBody
        response.headers shouldBe case.responseHeaders
    } else {
        (call.response is NetworkResponse.Error) shouldBe true
        val error = call.response as NetworkResponse.Error
        error.exceptionMessage shouldBe case.responseError
        error.stackTrace.isNotBlank() shouldBe true
    }
}

public data class RequestCase(
    public val url: String,
    public val scheme: String,
    public val host: String,
    public val port: Int?,
    public val endpoint: String,
    public val queryParams: String,
    public val method: NetworkMethod = NetworkMethod.GET,
    public val requestBody: String? = null,
    public val requestHeaders: List<NetworkHeader> = emptyList(),
    public val responseCode: Int = 200,
    public val responseBody: String = "ok",
    public val responseHeaders: List<NetworkHeader> = listOf(NetworkHeader("X-Contract", "response")),
    public val responseError: String? = null,
)
