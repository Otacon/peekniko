package peekniko.internal.export

import dev.mokkery.answering.calls
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.matcher.any
import dev.mokkery.mock
import dev.mokkery.verify
import dev.mokkery.verify.VerifyMode.Companion.exactly
import io.kotest.core.spec.style.FunSpec
import peekniko.NetworkCall
import peekniko.NetworkHeader
import peekniko.NetworkMethod
import peekniko.internal.bodyrenderer.BodyRenderer
import peekniko.internal.clipboard.Clipboard
import peekniko.internal.formatting.TimeFormatter
import peekniko.sampleCall
import peekniko.sampleError
import peekniko.sampleRequest
import peekniko.sampleResponse
import kotlin.time.Instant

internal class TextClipboardExporterTest : FunSpec({

    infix fun NetworkCall.shouldCopy(expected: String) {
        val clipboard = mock<Clipboard> { every { copy(any()) } returns true }
        val bodyRenderer = mock<BodyRenderer> {
            every { supports(any()) } calls { (contentType: String) -> contentType == "application/json" }
            every { format(any(), any()) } calls { (payload: String, _: String) -> "pretty $payload" }
        }
        val timeFormatter = mock<TimeFormatter> {
            every { format(any()) } calls { (instant: Instant) -> "t${instant.toEpochMilliseconds()}" }
        }
        TextClipboardExporter(clipboard, bodyRenderer, timeFormatter).export(this)
        verify(exactly(1)) { clipboard.copy(expected) }
    }

    test("formats a completed call") {
        sampleCall(
            request = sampleRequest(
                method = NetworkMethod.POST,
                endpoint = "/items",
                headers = listOf(NetworkHeader("Content-Type", "application/json")),
                body = """{"a":1}""".encodeToByteArray(),
            ),
            response = sampleResponse(
                code = 200,
                headers = listOf(NetworkHeader("Content-Type", "text/plain")),
                body = "done".encodeToByteArray(),
            ),
        ) shouldCopy """
            URL: https://example.com/items
            Method: POST
            Protocol: h2
            Status: Complete
            Response: 200 OK
            SSL: Yes

            Request time: t0
            Response time: t11
            Duration: 11 ms

            Request size: 7 B
            Response size: 4 B
            Total size: 11 B

            ---------- Request ----------

            Content-Type: application/json

            pretty {"a":1}

            ---------- Response ----------

            Content-Type: text/plain

            done
        """.trimIndent()
    }

    test("formats a pending call") {
        sampleCall(
            request = sampleRequest(scheme = "http", endpoint = "/items", headers = emptyList()),
            response = null,
        ) shouldCopy """
            URL: http://example.com/items
            Method: GET
            Status: Requested
            SSL: No

            Request time: t0

            Request size: 0 B
            Total size: 0 B

            ---------- Request ----------

            (body is empty)

            ---------- Response ----------

            (body is empty)
        """.trimIndent()
    }

    test("formats a failed call with its stack trace") {
        sampleCall(
            request = sampleRequest(endpoint = "/items", headers = emptyList()),
            response = sampleError(message = "connection failed", stackTrace = "trace line 1\ntrace line 2"),
        ) shouldCopy """
            URL: https://example.com/items
            Method: GET
            Status: Failed
            Response: connection failed
            SSL: Yes

            Request time: t0
            Response time: t789
            Duration: 789 ms

            Request size: 0 B
            Total size: 0 B

            ---------- Request ----------

            (body is empty)

            ---------- Response ----------

            trace line 1
            trace line 2
        """.trimIndent()
    }

    test("omits image and binary bodies") {
        sampleCall(
            request = sampleRequest(
                endpoint = "/upload",
                headers = emptyList(),
                body = byteArrayOf(0xFF.toByte(), 0xFE.toByte()),
            ),
            response = sampleResponse(
                code = 200,
                protocol = null,
                headers = listOf(NetworkHeader("Content-Type", "image/png")),
                body = byteArrayOf(1, 2, 3),
            ),
        ) shouldCopy """
            URL: https://example.com/upload
            Method: GET
            Status: Complete
            Response: 200 OK
            SSL: Yes

            Request time: t0
            Response time: t11
            Duration: 11 ms

            Request size: 2 B
            Response size: 3 B
            Total size: 5 B

            ---------- Request ----------

            (encoded or binary body omitted)

            ---------- Response ----------

            Content-Type: image/png

            (encoded or binary body omitted)
        """.trimIndent()
    }
})
