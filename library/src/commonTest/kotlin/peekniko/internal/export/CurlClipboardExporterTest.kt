package peekniko.internal.export

import io.kotest.core.spec.style.FunSpec
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.matcher.any
import dev.mokkery.mock
import dev.mokkery.verify
import dev.mokkery.verify.VerifyMode.Companion.exactly
import peekniko.NetworkHeader
import peekniko.NetworkMethod
import peekniko.NetworkRequest
import peekniko.internal.clipboard.Clipboard
import peekniko.sampleCall
import peekniko.sampleRequest

internal class CurlClipboardExporterTest : FunSpec({

    infix fun NetworkRequest.shouldCopy(expected: String) {
        val clipboard = mock<Clipboard> { every { copy(any()) } returns true }
        CurlClipboardExporter(clipboard).export(sampleCall(request = this))
        verify(exactly(1)) { clipboard.copy(expected) }
    }

    context("method") {
        NetworkMethod.entries.forEach { method ->
            val expected = when (method) {
                NetworkMethod.GET -> "curl 'https://example.com/'"
                NetworkMethod.HEAD -> "curl --head \\\n  'https://example.com/'"
                NetworkMethod.POST -> "curl -X POST \\\n  'https://example.com/'"
                NetworkMethod.PUT -> "curl -X PUT \\\n  'https://example.com/'"
                NetworkMethod.PATCH -> "curl -X PATCH \\\n  'https://example.com/'"
                NetworkMethod.DELETE -> "curl -X DELETE \\\n  'https://example.com/'"
                NetworkMethod.OPTIONS -> "curl -X OPTIONS \\\n  'https://example.com/'"
                NetworkMethod.TRACE -> "curl -X TRACE \\\n  'https://example.com/'"
                NetworkMethod.CONNECT -> "curl -X CONNECT \\\n  'https://example.com/'"
                NetworkMethod.UNKNOWN -> "curl 'https://example.com/'"
            }
            test("$method") {
                sampleRequest(
                    method = method,
                    endpoint = "/",
                    headers = emptyList()
                ) shouldCopy expected
            }
        }
    }

    test("includes query parameters") {
        sampleRequest(
            endpoint = "/items",
            queryParams = "page=2&sort=name&filter=a",
            headers = emptyList(),
        ) shouldCopy "curl 'https://example.com/items?page=2&sort=name&filter=a'"
    }

    test("includes the port") {
        sampleRequest(
            scheme = "http",
            host = "localhost",
            port = 8080,
            endpoint = "/",
            headers = emptyList()
        ) shouldCopy "curl 'http://localhost:8080/'"
    }

    test("formats method, headers and body on separate lines") {
        sampleRequest(
            method = NetworkMethod.POST,
            endpoint = "/items",
            headers = listOf(NetworkHeader("Content-Type", "application/json")),
            body = """{"name":"value"}""".encodeToByteArray(),
        ) shouldCopy "curl -X POST \\\n" +
                "  -H 'Content-Type: application/json' \\\n" +
                "  --data-raw '{\"name\":\"value\"}' \\\n" +
                "  'https://example.com/items'"
    }

    test("escapes single quotes") {
        sampleRequest(
            method = NetworkMethod.PUT,
            endpoint = "/",
            headers = listOf(NetworkHeader("X-Name", "O'Brien")),
            body = "it's".encodeToByteArray(),
        ) shouldCopy "curl -X PUT \\\n" +
                "  -H 'X-Name: O'\\''Brien' \\\n" +
                "  --data-raw 'it'\\''s' \\\n" +
                "  'https://example.com/'"
    }

    test("drops Content-Length") {
        sampleRequest(
            endpoint = "/",
            headers = listOf(NetworkHeader("Content-Length", "0"), NetworkHeader("Accept", "*/*")),
        ) shouldCopy "curl -H 'Accept: */*' \\\n  'https://example.com/'"
    }

    test("omits an empty body") {
        sampleRequest(
            method = NetworkMethod.POST,
            endpoint = "/", headers = emptyList(),
            body = ByteArray(0)
        ) shouldCopy "curl -X POST \\\n  'https://example.com/'"
    }
})
