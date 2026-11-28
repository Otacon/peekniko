package peekniko.internal.curl

import peekniko.NetworkMethod
import peekniko.NetworkRequest
import peekniko.fullUrl
import peekniko.internal.clipboard.Clipboard

internal interface CurlCommandCopier {

    fun copy(request: NetworkRequest)

}

internal class ClipboardCurlCommandCopier(
    private val clipboard: Clipboard
) : CurlCommandCopier {

    override fun copy(request: NetworkRequest) {
        val text = buildList {
            when (request.method) {
                NetworkMethod.GET,
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
            request.headers
                .filterNot { it.name.equals("Content-Length", ignoreCase = true) }
                .forEach { add("-H ${"${it.name}: ${it.value}".shellQuoted()}") }
            request.body
                ?.takeIf { it.isNotEmpty() }
                ?.let { add("--data-raw ${it.decodeToString().shellQuoted()}") }

            add(request.fullUrl.shellQuoted())
        }.joinToString(separator = " \\\n  ", prefix = "curl ")
        clipboard.copy(text)
    }

    private fun String.shellQuoted(): String = "'" + replace("'", "'\\''") + "'"
}
