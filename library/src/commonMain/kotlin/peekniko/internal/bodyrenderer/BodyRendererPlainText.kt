package peekniko.internal.bodyrenderer

import peekniko.ui.details.BodyContentType

internal class BodyRendererPlainText(
    private val spannedTextBuilderFactory: SpannedTextBuilderFactory,
) : BodyRenderer {
    override val contentType: BodyContentType = BodyContentType.PLAIN_TEXT

    override fun supports(contentType: String?): Boolean = true

    override fun format(payload: String, contentType: String?): String = payload

    override fun syntaxHighlight(theme: BodyRendererTheme, payload: String, contentType: String?): SpannedTextBuilder =
        spannedTextBuilderFactory.create().apply { append(payload) }
}
