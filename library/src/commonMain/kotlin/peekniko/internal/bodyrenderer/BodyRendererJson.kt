package peekniko.internal.bodyrenderer

import peekniko.internal.json.generated.JSONLexer
import peekniko.internal.json.generated.JSONLexer.Tokens
import org.antlr.v4.kotlinruntime.CharStreams
import org.antlr.v4.kotlinruntime.Token

internal class BodyRendererJson : BodyRenderer {
    override fun supports(contentType: String): Boolean {
        val mediaType = contentType.substringBefore(';').trim().lowercase()
        return mediaType == "application/json" || mediaType == "text/json" || mediaType.endsWith("+json")
    }

    override fun format(payload: String, contentType: String): String? =
        runCatching { formatTokens(tokenize(payload)) }.getOrNull()

    override fun syntaxHighlight(theme: BodyRendererTheme, payload: String, contentType: String): SpannedText? =
        runCatching { highlight(payload, theme) }.getOrNull()

    private fun tokenize(payload: String): Sequence<Token> {
        val lexer = JSONLexer(CharStreams.fromString(payload))
        lexer.removeErrorListeners()
        return generateSequence { lexer.nextToken().takeUnless { it.type == Token.EOF } }
    }

    private fun formatTokens(tokens: Sequence<Token>): String? {
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
            val text = token.text.orEmpty()
            val closes = type == Tokens.RBRACE || type == Tokens.RBRACKET
            if (closes) {
                nesting.removeLast()
                if (!justOpened) output.newline(nesting.size)
            } else if (justOpened) {
                output.newline(nesting.size)
            }
            when (type) {
                Tokens.KEY -> {
                    output.append(text, 0, text.lastIndexOf('"') + 1).append(": ")
                }

                Tokens.COMMA -> {
                    output.append(',')
                    output.newline(nesting.size)
                }

                else -> {
                    output.append(text)
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

    private fun highlight(payload: String, theme: BodyRendererTheme): SpannedText = spannedText {
        var cursor = 0
        for (token in tokenize(payload)) {
            append(payload.substring(cursor, token.startIndex))
            cursor = token.stopIndex + 1
            val tokenText = payload.substring(token.startIndex, cursor)
            if (token.type == Tokens.KEY) {
                val keyEnd = tokenText.lastIndexOf('"') + 1
                append(tokenText.substring(0, keyEnd), theme.key)
                append(tokenText.substring(keyEnd, tokenText.length - 1))
                append(":", theme.separator)
            } else {
                val color = when (token.type) {
                    Tokens.STRING -> theme.string
                    Tokens.NUMBER -> {
                        val text = token.text.orEmpty()
                        if (text.contains('.') || text.contains('e', ignoreCase = true)) theme.number else theme.integer
                    }

                    Tokens.BOOLEAN -> theme.boolean
                    Tokens.NULL -> theme.nullValue
                    else -> theme.separator
                }
                append(tokenText, color)
            }
        }
        append(payload.substring(cursor))
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
