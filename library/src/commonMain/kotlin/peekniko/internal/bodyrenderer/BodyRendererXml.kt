package peekniko.internal.bodyrenderer

import peekniko.internal.json.generated.XMLLexer
import peekniko.internal.json.generated.XMLLexer.Tokens
import peekniko.ui.details.BodyContentType
import org.antlr.v4.kotlinruntime.CharStreams

internal class BodyRendererXml : BodyRenderer {
    override val contentType: BodyContentType = BodyContentType.XML

    override fun supports(contentType: String?): Boolean {
        val mediaType = contentType?.substringBefore(';')?.trim()?.lowercase()
        return mediaType in setOf("application/xml", "text/xml") || mediaType?.endsWith("+xml") == true
    }

    override fun format(payload: String, contentType: String?): String =
        runCatching { MarkupRendererUtilities.format(payload, tokenize(payload)) }
            .getOrElse { payload }

    override fun syntaxHighlight(theme: BodyRendererTheme, payload: String, contentType: String?) = runCatching {
        spannedText {
            MarkupRendererUtilities.syntaxHighlight(
                payload = payload,
                tokens = tokenize(payload),
                theme = theme,
                output = this,
            )
        }
    }.getOrElse { spannedText { append(payload) } }

    private fun tokenize(payload: String): List<MarkupToken> =
        MarkupRendererUtilities.tokenize(payload, XMLLexer(CharStreams.fromString(payload))) { type ->
            when (type) {
                Tokens.OPEN -> "OPEN"
                Tokens.CLOSE, Tokens.SLASH_CLOSE -> "CLOSE"
                Tokens.PI -> "TAG"
                Tokens.COMMENT -> "COMMENT"
                Tokens.CDATA -> "CDATA"
                Tokens.DTD -> "DOCTYPE"
                else -> "TEXT"
            }
        }
}
