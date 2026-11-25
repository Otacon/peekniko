package peekniko

import mockwebserver3.Dispatcher
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.RecordedRequest
import mockwebserver3.SocketEffect
import okio.Buffer
import java.util.concurrent.TimeUnit

internal class MockApiServer : MockServer, AutoCloseable {
    private val server = MockWebServer()

    override val baseUrl
        get() = server.url("").toString()

    override fun start() {
        server.dispatcher = PlaygroundDispatcher
        server.start()
    }

    override fun close() {
        server.close()
    }
}

private object PlaygroundDispatcher : Dispatcher() {
    private val routes = listOf(
        MockRoute(
            "GET",
            "/get",
            200,
            "get",
            body = listItemsJson(),
        ),
        MockRoute(
            "POST",
            "/post",
            200,
            "post-json",
            body = mockResource("post-json.json"),
            requestContentType = "application/json",
        ),
        MockRoute(
            "POST",
            "/post",
            200,
            "post-form",
            body = mockResource("post-form.json"),
            requestContentType = "application/x-www-form-urlencoded",
        ),
        MockRoute(
            "PATCH",
            "/patch",
            200,
            "patch",
            body = mockResource("patch.json"),
        ),
        MockRoute(
            "PUT",
            "/put",
            200,
            "put",
            body = mockResource("put.json"),
        ),
        MockRoute(
            "GET",
            "/status/500",
            500,
            "status-500",
        ),
        MockRoute(
            "POST",
            "/status/422",
            422,
            "status-422",
        ),
        MockRoute(
            "GET",
            "/delay/1",
            200,
            "delay-1",
            body = mockResource("delay-1.json"),
            delaySeconds = 1,
        ),
        MockRoute(
            "GET",
            "/xml",
            200,
            "xml-catalog",
            body = mockResource("xml-catalog.xml"),
            contentType = "application/xml; charset=utf-8",
        ),
        MockRoute(
            "GET",
            "/html",
            200,
            "html-page",
            body = mockResource("html-page.html"),
            contentType = "text/html; charset=utf-8",
        ),
        MockRoute(
            "GET",
            "/robots.txt",
            200,
            "robots-txt",
            body = mockResource("robots.txt"),
            contentType = "text/plain; charset=utf-8",
        ),
        MockRoute(
            "GET",
            "/image/png",
            200,
            "image-png",
            body = mockResource("image.png"),
            contentType = "image/png",
        ),
        MockRoute(
            "GET",
            "/image/jpeg",
            200,
            "image-jpeg",
            body = mockResource("image.jpeg"),
            contentType = "image/jpeg",
        ),
        MockRoute(
            "GET",
            "/image/webp",
            200,
            "image-webp",
            body = mockResource("image.webp"),
            contentType = "image/webp",
        ),
        MockRoute(
            "GET",
            "/image/svg",
            200,
            "image-svg",
            body = mockResource("image.svg"),
            contentType = "image/svg+xml",
        ),
    )

    override fun dispatch(request: RecordedRequest): MockResponse {
        if (request.method == "GET" && request.url.encodedPath == "/connection-failure") {
            // Apply on every attempt so a transparent client retry cannot turn this into a success.
            return MockResponse.Builder()
                .onResponseStart(SocketEffect.CloseSocket())
                .build()
        }
        val route = routes.firstOrNull {
            it.method == request.method &&
                    it.path == request.url.encodedPath &&
                    (it.requestContentType == null || request.headers["Content-Type"]?.startsWith(it.requestContentType) == true)
        } ?: return MockResponse.Builder()
            .code(404)
            .addHeader("Content-Type", "application/json; charset=utf-8")
            .body("""{"error":"no mock route for ${request.method} ${request.url.encodedPath}"}""")
            .build()

        return MockResponse.Builder()
            .code(route.status)
            .addHeader("Content-Type", route.contentType)
            .addHeader("X-Mock-Scenario", route.scenario)
            .body(Buffer().write(route.body))
            .apply { if (route.delaySeconds > 0) bodyDelay(route.delaySeconds, TimeUnit.SECONDS) }
            .build()
    }
}

private data class MockRoute(
    val method: String,
    val path: String,
    val status: Int,
    val scenario: String,
    val body: ByteArray = ByteArray(0),
    val delaySeconds: Long = 0,
    val contentType: String = "application/json; charset=utf-8",
    val requestContentType: String? = null,
)

private fun mockResource(name: String): ByteArray =
    checkNotNull(MockApiServer::class.java.getResourceAsStream("/mockserver/$name")) { "Missing resource mockserver/$name" }
        .use { it.readBytes() }

private fun listItemsJson(): ByteArray {
    val count = 100
    val item = mockResource("get-item.template.json").decodeToString()
    val items = (0 until count).joinToString(",") { item.replace("{{id}}", it.toString()) }
    return mockResource("get.template.json").decodeToString()
        .replace("{{total}}", count.toString())
        .replace("{{items}}", items)
        .encodeToByteArray()
}
