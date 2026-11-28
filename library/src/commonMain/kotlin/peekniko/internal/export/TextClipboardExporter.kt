package peekniko.internal.export

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import peekniko.NetworkCall
import peekniko.NetworkHeader
import peekniko.NetworkResponse
import peekniko.fullUrl
import peekniko.internal.bodyrenderer.BodyRenderer
import peekniko.internal.clipboard.Clipboard
import peekniko.internal.formatting.TimeFormatter
import peekniko.internal.formatting.formatPayloadSize
import peekniko.internal.http.contentType
import peekniko.internal.http.httpMessage
import peekniko.isSsl

internal class TextClipboardExporter(
    private val clipboard: Clipboard,
    private val bodyRenderer: BodyRenderer,
    private val timeFormatter: TimeFormatter,
    private val backgroundDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : ClipboardExporter {

    override suspend fun export(call: NetworkCall) {
        val text = withContext(backgroundDispatcher) { call.toText() }
        clipboard.copy(text)
    }

    private fun NetworkCall.toText(): String {
        val response = response as? NetworkResponse.Response
        val error = this.response as? NetworkResponse.Error
        val completedAt = this.response?.timestamp
        val requestSize = request.body?.size?.toLong() ?: 0L
        val responseSize = response?.let { it.body?.size?.toLong() ?: 0L }

        val overview = listOf(
            "URL" to request.fullUrl,
            "Method" to request.method.name,
            "Protocol" to response?.protocol,
            "Status" to status,
            "Response" to (error?.exceptionMessage ?: response?.summary),
            "SSL" to if (request.isSsl) "Yes" else "No",
        )
        val timings = listOf(
            "Request time" to timeFormatter.format(request.timestamp),
            "Response time" to completedAt?.let(timeFormatter::format),
            "Duration" to completedAt?.let { "${(it - request.timestamp).inWholeMilliseconds.coerceAtLeast(0L)} ms" },
        )
        val sizes = listOf(
            "Request size" to requestSize.formatPayloadSize(),
            "Response size" to responseSize?.formatPayloadSize(),
            "Total size" to (requestSize + (responseSize ?: 0L)).formatPayloadSize(),
        )

        return buildString {
            listOf(overview, timings, sizes).forEach { section ->
                section.forEach { (name, value) -> if (value != null) append("$name: $value\n") }
                append("\n")
            }
            append("---------- Request ----------\n\n")
            appendHeadersAndBody(request.headers, request.body)
            append("\n\n")
            append("---------- Response ----------\n\n")
            if (error != null) {
                append(error.stackTrace.ifBlank { BODY_EMPTY })
            } else {
                appendHeadersAndBody(response?.headers.orEmpty(), response?.body)
            }
        }
    }

    private val NetworkCall.status: String
        get() = when (response) {
            is NetworkResponse.Error -> "Failed"
            is NetworkResponse.Response -> "Complete"
            null -> "Requested"
        }

    private val NetworkResponse.Response.summary: String
        get() = code.httpMessage()?.let { "$code $it" } ?: code.toString()

    private fun StringBuilder.appendHeadersAndBody(headers: List<NetworkHeader>, body: ByteArray?) {
        if (headers.isNotEmpty()) {
            headers.forEach { append("${it.name}: ${it.value}\n") }
            append("\n")
        }
        append(formatBody(headers.contentType(), body))
    }

    private fun formatBody(contentType: String?, body: ByteArray?): String {
        if (body == null || body.isEmpty()) return BODY_EMPTY
        val text = decodeTextBody(body, contentType) ?: return BODY_OMITTED
        if (text.isBlank()) return BODY_EMPTY
        val supportedContentType = contentType?.takeIf(bodyRenderer::supports) ?: return text
        return bodyRenderer.format(text, supportedContentType) ?: text
    }

    private companion object {
        const val BODY_EMPTY = "(body is empty)"
        const val BODY_OMITTED = "(encoded or binary body omitted)"
    }
}
