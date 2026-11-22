package peekniko.internal.bodyrenderer

import peekniko.internal.json.generated.HTMLLexer
import peekniko.internal.json.generated.HTMLLexer.Tokens
import org.antlr.v4.kotlinruntime.CharStreams

internal class BodyRendererHtml : BodyRenderer {
    override fun supports(contentType: String): Boolean {
        val mediaType = contentType.substringBefore(';').trim().lowercase()
        return mediaType in setOf("text/html", "application/html", "application/xhtml+xml")
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
        HTMLLexer(CharStreams.fromString(payload))
    ) { type ->
        when (type) {
            Tokens.TAG_OPEN -> "OPEN"
            Tokens.TAG_CLOSE, Tokens.TAG_SLASH_CLOSE -> "CLOSE"
            Tokens.SCRIPT_OPEN, Tokens.STYLE_OPEN, Tokens.XML, Tokens.SCRIPTLET -> "TAG"
            Tokens.HTML_COMMENT, Tokens.HTML_CONDITIONAL_COMMENT -> "COMMENT"
            Tokens.CDATA -> "CDATA"
            Tokens.DTD -> "DOCTYPE"
            else -> "TEXT"
        }
    }
}
