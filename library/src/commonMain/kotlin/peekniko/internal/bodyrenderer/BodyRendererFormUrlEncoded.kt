package peekniko.internal.bodyrenderer

import peekniko.internal.json.generated.FormUrlEncodedLexer
import peekniko.internal.json.generated.FormUrlEncodedLexer.Tokens
import org.antlr.v4.kotlinruntime.CharStream
import org.antlr.v4.kotlinruntime.Lexer
import org.antlr.v4.kotlinruntime.Token

internal class BodyRendererFormUrlEncoded : BodyRenderer {
    private val tokenizer = BodyTokenizer(FormUrlEncodedGrammar)

    override fun supports(contentType: String): Boolean {
        val mediaType = contentType.substringBefore(';').trim()
        return mediaType.equals("application/x-www-form-urlencoded", ignoreCase = true)
    }

    override fun format(payload: String, contentType: String): String? = runCatching {
        buildString {
            for (token in tokenizer.tokens(payload)) {
                append(
                    when (token.type) {
                        Tokens.AMPERSAND, Tokens.LINE_BREAK -> "\n"
                        Tokens.EQUALS -> " = "
                        Tokens.PLUS -> " "
                        else -> payload.substring(token.startIndex, token.endIndex)
                    }
                )
            }
        }
    }.getOrNull()

    override fun syntaxHighlight(theme: BodyRendererTheme, payload: String, contentType: String): SpannedText? =
        runCatching { tokenizer.highlight(payload, theme) }.getOrNull()
}

private object FormUrlEncodedGrammar : TokenGrammar {
    override fun lexer(input: CharStream): Lexer = FormUrlEncodedLexer(input)

    override fun highlight(payload: String, tokens: Sequence<Token>, theme: BodyRendererTheme, color: ColorSink) {
        var isValue = false
        for (token in tokens) {
            when (token.type) {
                Tokens.AMPERSAND -> {
                    isValue = false
                    color(token.startIndex, token.endIndex, theme.separator)
                }

                Tokens.LINE_BREAK -> isValue = false

                Tokens.EQUALS -> {
                    isValue = true
                    color(token.startIndex, token.endIndex, theme.separator)
                }

                else -> color(token.startIndex, token.endIndex, if (isValue) theme.attributeValue else theme.key)
            }
        }
    }
}
