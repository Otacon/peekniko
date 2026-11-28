package peekniko.internal.export

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import peekniko.NetworkCall
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
        val command = buildList {
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
                .filterNot { it.name.equals("Content-Length", ignoreCase = true) }
                .forEach { add("-H ${"${it.name}: ${it.value}".shellQuoted()}") }
            bodyText?.let { add("--data-raw ${it.shellQuoted()}") }
            if (url.any { it in GLOB_CHARACTERS }) {
                add("--globoff")
            }
            add(url.shellQuoted())
        }.joinToString(separator = " \\\n  ", prefix = "curl ")
        return if (bodyBytes != null && bodyText == null) "$command\n$BINARY_BODY_OMITTED" else command
    }

    private fun String.shellQuoted(): String = "'" + replace("'", "'\\''") + "'"

    private companion object {
        const val GLOB_CHARACTERS = "[]{}"
        const val BINARY_BODY_OMITTED = "# encoded or binary request body omitted"
    }
}
