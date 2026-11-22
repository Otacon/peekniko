package peekniko.internal.bodyrenderer

import peekniko.internal.json.generated.XMLLexer
import peekniko.internal.json.generated.XMLLexer.Tokens
import org.antlr.v4.kotlinruntime.CharStream
import org.antlr.v4.kotlinruntime.Lexer

internal class BodyRendererXml : BodyRenderer {
    private val tokenizer = BodyTokenizer(XmlGrammar)

    override fun supports(contentType: String): Boolean {
        val mediaType = contentType.substringBefore(';').trim().lowercase()
        return mediaType in setOf("application/xml", "text/xml") || mediaType.endsWith("+xml")
    }

    override fun format(payload: String, contentType: String): String? = runCatching {
        MarkupRendererUtilities.format(payload, XmlGrammar.tokenize(payload, tokenizer.tokens(payload)))
    }.getOrNull()

    override fun syntaxHighlight(theme: BodyRendererTheme, payload: String, contentType: String): SpannedText? =
        runCatching { tokenizer.highlight(payload, theme) }.getOrNull()
}

private object XmlGrammar : MarkupGrammar {
    override fun lexer(input: CharStream): Lexer = XMLLexer(input)

    override fun kindOf(type: Int): MarkupKind = when (type) {
        Tokens.OPEN -> MarkupKind.Open
        Tokens.CLOSE, Tokens.SLASH_CLOSE -> MarkupKind.Close
        Tokens.PI -> MarkupKind.Tag
        Tokens.COMMENT -> MarkupKind.Comment
        Tokens.CDATA -> MarkupKind.Cdata
        Tokens.DTD -> MarkupKind.Doctype
        else -> MarkupKind.Text
    }
}
