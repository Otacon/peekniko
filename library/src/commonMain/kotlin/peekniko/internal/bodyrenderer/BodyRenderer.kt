package peekniko.internal.bodyrenderer

internal interface BodyRenderer {
    fun supports(contentType: String): Boolean
    fun format(payload: String, contentType: String): String?
    fun syntaxHighlight(theme: BodyRendererTheme, payload: String, contentType: String): SpannedText?
}
