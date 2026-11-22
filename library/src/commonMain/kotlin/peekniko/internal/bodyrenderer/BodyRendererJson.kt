package peekniko.internal.bodyrenderer

import peekniko.internal.json.generated.JSONLexer
import peekniko.internal.json.generated.JSONLexer.Tokens
import org.antlr.v4.kotlinruntime.CharStream
import org.antlr.v4.kotlinruntime.Lexer
import org.antlr.v4.kotlinruntime.Token

internal class BodyRendererJson : BodyRenderer {
    private val tokenizer = BodyTokenizer(JsonGrammar)

    override fun supports(contentType: String): Boolean {
        val mediaType = contentType.substringBefore(';').trim().lowercase()
        return mediaType == "application/json" || mediaType == "text/json" || mediaType.endsWith("+json")
    }

    override fun format(payload: String, contentType: String): String? =
        runCatching { formatTokens(payload, tokenizer.tokens(payload)) }.getOrNull()

    override fun syntaxHighlight(theme: BodyRendererTheme, payload: String, contentType: String): SpannedText? =
        runCatching { tokenizer.highlight(payload, theme) }.getOrNull()

    private fun formatTokens(payload: String, tokens: Sequence<Token>): String? {
        val output = StringBuilder()
        // The opening brackets (Tokens.LBRACE or Tokens.LBRACKET) enclosing the current token, innermost last.
        val nesting = ArrayDeque<Int>()
        var allowed = VALUE_START
        var justOpened = false
        tokens.forEach { token ->
            val type = token.type
            if (type !in allowed) {
                return null
            }
            val closes = type == Tokens.RBRACE || type == Tokens.RBRACKET
            if (closes) {
                nesting.removeLast()
                if (!justOpened) output.newline(nesting.size)
            } else if (justOpened) {
                output.newline(nesting.size)
            }
            when (type) {
                Tokens.KEY -> {
                    output.append(payload, token.startIndex, payload.lastIndexOf('"', token.endIndex - 1) + 1).append(": ")
                }

                Tokens.COMMA -> {
                    output.append(',')
                    output.newline(nesting.size)
                }

                else -> {
                    output.append(payload, token.startIndex, token.endIndex)
                }
            }
            justOpened = type == Tokens.LBRACE || type == Tokens.LBRACKET
            if (justOpened) nesting.addLast(type)
            allowed = allowedAfter(type, nesting)
        }
        return if (allowed.isEmpty()) output.toString() else null
    }

    private fun allowedAfter(type: Int, nesting: ArrayDeque<Int>): Set<Int> = when (type) {
        Tokens.LBRACE -> KEY_OR_CLOSE_OBJECT
        Tokens.LBRACKET -> VALUE_OR_CLOSE_ARRAY
        Tokens.KEY -> VALUE_START
        Tokens.COMMA -> if (nesting.last() == Tokens.LBRACE) {
            KEY_ONLY
        } else {
            VALUE_START
        }

        else -> when (nesting.lastOrNull()) {
            null -> NOTHING
            Tokens.LBRACE -> COMMA_OR_CLOSE_OBJECT
            else -> COMMA_OR_CLOSE_ARRAY
        }
    }

    private fun StringBuilder.newline(depth: Int) {
        append('\n')
        repeat(depth * INDENT) { append(' ') }
    }

}

private object JsonGrammar : TokenGrammar {
    override fun lexer(input: CharStream): Lexer = JSONLexer(input)

    override fun highlight(payload: String, tokens: Sequence<Token>, theme: BodyRendererTheme, color: ColorSink) {
        for (token in tokens) {
            if (token.type == Tokens.KEY) {
                color(token.startIndex, payload.lastIndexOf('"', token.endIndex - 1) + 1, theme.key)
                color(token.endIndex - 1, token.endIndex, theme.separator)
            } else {
                color(token.startIndex, token.endIndex, colorOf(payload, token, theme))
            }
        }
    }

    private fun colorOf(payload: String, token: Token, theme: BodyRendererTheme): Color = when (token.type) {
        Tokens.STRING -> theme.string
        Tokens.NUMBER -> if (isInteger(payload, token)) theme.integer else theme.number
        Tokens.BOOLEAN -> theme.boolean
        Tokens.NULL -> theme.nullValue
        else -> theme.separator
    }

    private fun isInteger(payload: String, token: Token): Boolean {
        for (index in token.startIndex until token.endIndex) {
            val char = payload[index]
            if (char == '.' || char == 'e' || char == 'E') return false
        }
        return true
    }
}

private const val INDENT = 2

private val VALUE_START = setOf(
    Tokens.LBRACE, Tokens.LBRACKET, Tokens.STRING, Tokens.NUMBER, Tokens.BOOLEAN, Tokens.NULL
)
private val VALUE_OR_CLOSE_ARRAY = VALUE_START + Tokens.RBRACKET
private val KEY_ONLY = setOf(Tokens.KEY)
private val KEY_OR_CLOSE_OBJECT = setOf(Tokens.KEY, Tokens.RBRACE)
private val COMMA_OR_CLOSE_OBJECT = setOf(Tokens.COMMA, Tokens.RBRACE)
private val COMMA_OR_CLOSE_ARRAY = setOf(Tokens.COMMA, Tokens.RBRACKET)
private val NOTHING = emptySet<Int>()
