package peekniko.internal.bodyrenderer

import org.antlr.v4.kotlinruntime.CharStream
import org.antlr.v4.kotlinruntime.IntStream
import org.antlr.v4.kotlinruntime.Lexer
import org.antlr.v4.kotlinruntime.Token
import org.antlr.v4.kotlinruntime.misc.Interval

internal val Token.endIndex: Int get() = stopIndex + 1

internal fun interface ColorSink {
    operator fun invoke(start: Int, end: Int, color: Color)
}

internal interface TokenGrammar {
    fun lexer(input: CharStream): Lexer

    fun highlight(payload: String, tokens: Sequence<Token>, theme: BodyRendererTheme, color: ColorSink)
}

internal class BodyTokenizer(
    private val grammar: TokenGrammar
) {

    fun tokens(payload: String): Sequence<Token> {
        val lexer = grammar.lexer(Utf16CharStream(payload))
        lexer.removeErrorListeners()
        return generateSequence { lexer.nextToken().takeUnless { it.type == Token.EOF } }
    }

    fun highlight(payload: String, theme: BodyRendererTheme): SpannedText = spannedText {
        var cursor = 0
        grammar.highlight(payload, tokens(payload), theme) { start, end, color ->
            if (start > cursor) append(payload.substring(cursor, start))
            append(payload.substring(start, end), color)
            cursor = end
        }
        if (cursor < payload.length) append(payload.substring(cursor))
    }
}

private class Utf16CharStream(private val text: String) : CharStream {
    private var position = 0

    override val sourceName: String = IntStream.UNKNOWN_SOURCE_NAME

    override fun consume() {
        check(position < text.length) { "cannot consume EOF" }
        position++
    }

    override fun LA(i: Int): Int {
        val index = when {
            i > 0 -> position + i - 1
            i < 0 -> position + i
            else -> return 0
        }
        return if (index in text.indices) text[index].code else IntStream.EOF
    }

    override fun mark(): Int = -1

    override fun release(marker: Int) = Unit

    override fun index(): Int = position

    override fun seek(index: Int) {
        position = index.coerceIn(0, text.length)
    }

    override fun size(): Int = text.length

    override fun getText(interval: Interval): String {
        val start = interval.a.coerceIn(0, text.length)
        val end = (interval.b + 1).coerceIn(start, text.length)
        return text.substring(start, end)
    }
}
