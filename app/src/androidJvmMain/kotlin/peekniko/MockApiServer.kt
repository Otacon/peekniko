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
            """{"args":{"scenario":"list-items"},"url":"http://localhost/get"}"""
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
