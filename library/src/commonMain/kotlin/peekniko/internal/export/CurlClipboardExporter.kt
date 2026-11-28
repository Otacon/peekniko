package peekniko.internal.export

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import peekniko.NetworkCall
import peekniko.NetworkHeader
import peekniko.NetworkMethod
import peekniko.NetworkRequest
import peekniko.fullUrl
import peekniko.internal.clipboard.Clipboard
import peekniko.internal.http.contentType

internal class CurlClipboardExporter(
    private val clipboard: Clipboard,
    private val backgroundDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : ClipboardExporter {

    override suspend fun export(call: NetworkCall) {
        val text = withContext(backgroundDispatcher) { call.request.toCurlCommand() }
        clipboard.copy(text)
    }

    private fun NetworkRequest.toCurlCommand(): String {
        val bodyBytes = body?.takeIf { it.isNotEmpty() && method != NetworkMethod.HEAD }
        val bodyText = bodyBytes?.let { decodeTextBody(it, headers.contentType()) }
        val sendsBody = bodyText != null
        val url = fullUrl
        return buildList {
            when (method) {
                NetworkMethod.GET -> if (sendsBody) add("-X GET")
                NetworkMethod.UNKNOWN -> Unit
                NetworkMethod.HEAD -> add("--head")
                NetworkMethod.POST -> add("-X POST")
                NetworkMethod.PUT -> add("-X PUT")
                NetworkMethod.PATCH -> add("-X PATCH")
                NetworkMethod.DELETE -> add("-X DELETE")
                NetworkMethod.OPTIONS -> add("-X OPTIONS")
                NetworkMethod.TRACE -> add("-X TRACE")
                NetworkMethod.CONNECT -> add("-X CONNECT")
            }
            headers
                .filterNot { header -> DROPPED_HEADERS.any { it.equals(header.name, ignoreCase = true) } }
                .forEach { add("-H ${it.toCurlHeader().shellQuoted()}") }
            bodyText?.let { add("--data-raw ${it.shellQuoted()}") }
            if (url.any { it in GLOB_CHARACTERS }) {
                add("--globoff")
            }
            add(url.shellQuoted())
        }.joinToString(separator = " \\\n  ", prefix = "curl ")
    }

    // `-H 'Name:'` tells curl to remove the header; `-H 'Name;'` sends it with an empty value.
    private fun NetworkHeader.toCurlHeader(): String = if (value.isBlank()) "$name;" else "$name: $value"

    private fun String.shellQuoted(): String = "'" + replace("'", "'\\''") + "'"

    private companion object {
        const val GLOB_CHARACTERS = "[]{}"
        // curl computes Content-Length itself; Accept-Encoding would make the server send a compressed response.
        val DROPPED_HEADERS = listOf("Content-Length", "Accept-Encoding")
    }
}
