package peekniko.internal.bodyrenderer

import peekniko.internal.json.generated.XMLLexer
import peekniko.internal.json.generated.XMLLexer.Tokens
import org.antlr.v4.kotlinruntime.CharStreams

internal class BodyRendererXml : BodyRenderer {
    override fun supports(contentType: String): Boolean {
        val mediaType = contentType.substringBefore(';').trim().lowercase()
        return mediaType in setOf("application/xml", "text/xml") || mediaType.endsWith("+xml")
    }

    override fun format(payload: String, contentType: String): String? = runCatching {
        MarkupRendererUtilities.format(payload, tokenize(payload))
    }.getOrNull()

    override fun syntaxHighlight(theme: BodyRendererTheme, payload: String, contentType: String) = runCatching {
        spannedText {
            MarkupRendererUtilities.syntaxHighlight(
                payload = payload,
                tokens = tokenize(payload).toList(),
                theme = theme,
                output = this,
            )
        }
    }.getOrNull()

    private fun tokenize(payload: String): Sequence<MarkupToken> = MarkupRendererUtilities.tokenize(
        payload,
        XMLLexer(CharStreams.fromString(payload))
    ) { type ->
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
