package peekniko

import mockwebserver3.Dispatcher
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.RecordedRequest
import mockwebserver3.SocketEffect
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
            largeListItemsJson,
        ),
        MockRoute(
            "POST",
            "/post",
            200,
            "post-json",
            """{"json":{"name":"created from playground","active":true}}""",
            requestContentType = "application/json",
        ),
        MockRoute(
            "POST",
            "/post",
            200,
            "post-form",
            """{"form":{"name":"playground user","status":"ready"}}""",
            requestContentType = "application/x-www-form-urlencoded",
        ),
        MockRoute(
            "PATCH",
            "/patch",
            200,
            "patch",
            """{"json":{"name":"updated from playground"}}"""
        ),
        MockRoute(
            "PUT",
            "/put",
            200,
            "put",
            """{"form":{"name":"playground item","source":"desktop-android-app"}}"""
        ),
        MockRoute("GET", "/status/500", 500, "status-500", ""),
        MockRoute("POST", "/status/422", 422, "status-422", ""),
        MockRoute(
            "GET",
            "/delay/1",
            200,
            "delay-1",
            """{"args":{},"url":"http://localhost/delay/1"}""",
            1
        ),
        MockRoute(
            "GET",
            "/xml",
            200,
            "xml-catalog",
            """<?xml version="1.0" encoding="UTF-8"?><catalog><book id="b1"><title>ANTLR Kotlin</title><available>true</available></book><book id="b2"><title>Peekniko</title><available>false</available></book></catalog>""",
            contentType = "application/xml; charset=utf-8",
        ),
        MockRoute(
            "GET",
            "/html",
            200,
            "html-page",
            """<!doctype html><html><head><title>Playground</title></head><body><main class="content"><h1>Hello from MockWebServer</h1><p>HTML formatting and highlighting <strong>example</strong>.</p><img src="/logo.png" alt="logo"></main></body></html>""",
            contentType = "text/html; charset=utf-8",
        ),
        MockRoute(
            "GET",
            "/robots.txt",
            200,
            "robots-txt",
            "User-agent: *\nDisallow: /deny\n",
            contentType = "text/plain; charset=utf-8",
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
            .body(route.body)
            .apply { if (route.delaySeconds > 0) bodyDelay(route.delaySeconds, TimeUnit.SECONDS) }
            .build()
    }
}

private val largeListItemsJson: String by lazy {
    val itemCount = 100
    val statuses = listOf("active", "pending", "archived", "suspended")
    val tags = listOf("alpha", "beta", "gamma", "delta", "epsilon", "zeta")
    buildString {
        append("{\"args\":{\"scenario\":\"list-items\"},\"url\":\"http://localhost/get\",")
        append("\"total\":$itemCount,\"items\":[")
        for (i in 0 until itemCount) {
            if (i > 0) append(',')
            val uuidSuffix = i.toString().padStart(12, '0')
            val price = "${i * 7 % 1000}.${(i * 13 % 100).toString().padStart(2, '0')}"
            val rating = if (i % 10 == 0) "null" else (i % 5 + 1).toString()
            append('{')
            append("\"id\":$i,")
            append("\"uuid\":\"00000000-0000-4000-8000-$uuidSuffix\",")
            append("\"name\":\"Item #$i\",")
            append("\"status\":\"${statuses[i % statuses.size]}\",")
            append("\"active\":${i % 3 != 0},")
            append("\"price\":$price,")
            append("\"rating\":$rating,")
            append("\"tags\":[\"${tags[i % tags.size]}\",\"${tags[(i + 2) % tags.size]}\"],")
            append("\"owner\":{\"id\":${i % 97},\"email\":\"user${i % 97}@example.com\",\"verified\":${i % 2 == 0}},")
            append("\"dimensions\":{\"width\":${i % 50 + 10},\"height\":${i % 30 + 5},\"unit\":\"cm\"},")
            append("\"description\":\"Generated playground item number $i with escaped \\\"quotes\\\" and unicode \\u00e9 to stress the JSON renderer.\"")
            append('}')
        }
        append("]}")
    }
}

private data class MockRoute(
    val method: String,
    val path: String,
    val status: Int,
    val scenario: String,
    val body: String,
    val delaySeconds: Long = 0,
    val contentType: String = "application/json; charset=utf-8",
    val requestContentType: String? = null,
)
