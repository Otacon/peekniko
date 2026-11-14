package peekniko.internal.bodyrenderer

import peekniko.ui.details.BodyContentType

internal class BodyRendererComposite(
    private val renderers: List<BodyRenderer>,
    private val fallback: BodyRenderer,
) {
    fun renderer(contentType: String?): BodyRenderer =
        renderers.firstOrNull { it.supports(contentType) } ?: fallback

    fun contentType(contentType: String?): BodyContentType =
        renderer(contentType).contentType

    fun format(payload: String, contentType: String?): String =
        renderer(contentType).format(payload, contentType)

    fun syntaxHighlight(theme: BodyRendererTheme, payload: String, contentType: String?): SpannedText =
        renderer(contentType).syntaxHighlight(theme, payload, contentType).build()
}
