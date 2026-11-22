package peekniko.internal.bodyrenderer

import peekniko.internal.json.generated.HTMLLexer
import peekniko.internal.json.generated.HTMLLexer.Tokens
import org.antlr.v4.kotlinruntime.CharStream
import org.antlr.v4.kotlinruntime.Lexer

internal class BodyRendererHtml : BodyRenderer {
    private val tokenizer = BodyTokenizer(HtmlGrammar)

    override fun supports(contentType: String): Boolean {
        val mediaType = contentType.substringBefore(';').trim().lowercase()
        return mediaType in setOf("text/html", "application/html", "application/xhtml+xml")
    }

    override fun format(payload: String, contentType: String): String? = runCatching {
        MarkupRendererUtilities.format(payload, HtmlGrammar.tokenize(payload, tokenizer.tokens(payload)))
    }.getOrNull()

    override fun syntaxHighlight(theme: BodyRendererTheme, payload: String, contentType: String): SpannedText? =
        runCatching { tokenizer.highlight(payload, theme) }.getOrNull()
}

/** How HTML is lexed, and what its token types are. */
private object HtmlGrammar : MarkupGrammar {
    override fun lexer(input: CharStream): Lexer = HTMLLexer(input)

    override fun kindOf(type: Int): MarkupKind = when (type) {
        Tokens.TAG_OPEN -> MarkupKind.Open
        Tokens.TAG_CLOSE, Tokens.TAG_SLASH_CLOSE -> MarkupKind.Close
        Tokens.SCRIPT_OPEN, Tokens.STYLE_OPEN, Tokens.XML, Tokens.SCRIPTLET -> MarkupKind.Tag
        Tokens.HTML_COMMENT, Tokens.HTML_CONDITIONAL_COMMENT -> MarkupKind.Comment
        Tokens.CDATA -> MarkupKind.Cdata
        Tokens.DTD -> MarkupKind.Doctype
        else -> MarkupKind.Text
    }
}
