package peekniko.internal.bodyrenderer

internal class BodyRendererComposite(
    private val renderers: List<BodyRenderer>,
) : BodyRenderer {

    override fun supports(contentType: String): Boolean = renderers.any { it.supports(contentType) }

    override fun format(payload: String, contentType: String): String? =
        renderer(contentType)?.format(payload, contentType)

    override fun syntaxHighlight(theme: BodyRendererTheme, payload: String, contentType: String): SpannedText? =
        renderer(contentType)?.syntaxHighlight(theme, payload, contentType)

    private fun renderer(contentType: String): BodyRenderer? =
        renderers.firstOrNull { it.supports(contentType) }
}
