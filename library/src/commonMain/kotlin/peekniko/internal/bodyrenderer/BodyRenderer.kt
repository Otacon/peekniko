package peekniko.internal.bodyrenderer

import peekniko.ui.details.BodyContentType

internal interface BodyRenderer {
    val contentType: BodyContentType
    fun supports(contentType: String?): Boolean
    fun format(payload: String, contentType: String?): String
    fun syntaxHighlight(theme: BodyRendererTheme, payload: String, contentType: String?): SpannedText
}
