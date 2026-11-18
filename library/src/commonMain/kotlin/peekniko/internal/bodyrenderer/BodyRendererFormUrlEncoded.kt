package peekniko.internal.bodyrenderer

import peekniko.internal.json.generated.FormUrlEncodedLexer
import peekniko.internal.json.generated.FormUrlEncodedLexer.Tokens
import org.antlr.v4.kotlinruntime.CharStreams
import org.antlr.v4.kotlinruntime.Token

internal class BodyRendererFormUrlEncoded : BodyRenderer {
    override fun supports(contentType: String): Boolean {
        val mediaType = contentType.substringBefore(';').trim()
        return mediaType.equals("application/x-www-form-urlencoded", ignoreCase = true)
    }


    override fun format(payload: String, contentType: String): String = buildString {
        for (token in tokenize(payload)) {
            append(
                when (token.type) {
                    Tokens.AMPERSAND, Tokens.LINE_BREAK -> "\n"
                    Tokens.EQUALS -> " = "
                    Tokens.PLUS -> " "
                    else -> token.text
                }
            )
        }
    }

    override fun syntaxHighlight(theme: BodyRendererTheme, payload: String, contentType: String) = spannedText {
        var isValue = false
        for (token in tokenize(payload)) {
            val color = when (token.type) {
                Tokens.AMPERSAND, Tokens.LINE_BREAK -> {
                    isValue = false
                    if (token.type == Tokens.AMPERSAND) theme.separator else null
                }

                Tokens.EQUALS -> {
                    isValue = true
                    theme.separator
                }

                else -> if (isValue) theme.attributeValue else theme.key
            }
            append(token.text.orEmpty(), color)
        }
    }

    private fun tokenize(payload: String): Sequence<Token> {
        val lexer = FormUrlEncodedLexer(CharStreams.fromString(payload))
        return generateSequence { lexer.nextToken().takeUnless { it.type == Token.EOF } }
    }
}
