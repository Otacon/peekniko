package peekniko.internal.bodyrenderer

import org.antlr.v4.kotlinruntime.Lexer
import org.antlr.v4.kotlinruntime.Token

internal data class MarkupToken(
    val start: Int,
    val end: Int,
    val text: String,
    val kind: String,
)

internal object MarkupRendererUtilities {
    fun tokenize(payload: String, lexer: Lexer, kindOf: (Int) -> String): Sequence<MarkupToken> = sequence {
        lexer.removeErrorListeners()
        var tagStart: Int? = null
        var sourceIndex = 0
        var textIndex = 0
        var token = lexer.nextToken()
        while (token.type != Token.EOF) {
            // ANTLR indexes code points; Kotlin strings index UTF-16 code units.
            while (sourceIndex < token.startIndex) {
                textIndex += if (payload[textIndex].isHighSurrogate() &&
                    payload.getOrNull(textIndex + 1)?.isLowSurrogate() == true) 2 else 1
                sourceIndex++
            }
            val tokenStart = textIndex
            val tokenText = token.text.orEmpty()
            textIndex += tokenText.length
            sourceIndex = token.stopIndex + 1
            val tokenEnd = textIndex - 1
            val kind = kindOf(token.type)
            val start = tagStart
            when {
                kind == "OPEN" -> tagStart = tokenStart
                start != null && kind == "CLOSE" -> {
                    yield(MarkupToken(start, tokenEnd, payload.substring(start, textIndex), "TAG"))
                    tagStart = null
                }
                start == null -> yield(MarkupToken(tokenStart, tokenEnd, tokenText, kind))
            }
            token = lexer.nextToken()
        }
        tagStart?.let { yield(MarkupToken(it, payload.lastIndex, payload.substring(it), "TAG")) }
    }

    private val VOID_ELEMENTS = setOf(
        "area", "base", "br", "col", "embed", "hr", "img", "input", "link", "meta", "param", "source", "track", "wbr",
    )

    fun format(payload: String, tokens: Sequence<MarkupToken>): String = formatTokens(payload, tokens)

    fun syntaxHighlight(
        payload: String,
        tokens: List<MarkupToken>,
        theme: BodyRendererTheme,
        output: SpannedTextBuilder,
    ) = highlightTokens(payload, tokens, theme, output)

    private fun formatTokens(payload: String, tokens: Sequence<MarkupToken>): String {
        val output = StringBuilder()
        var depth = 0
        var cursor = 0
        for ((start, end, text1, kind) in tokens) {
            if (start > cursor) appendText(output, payload.substring(cursor, start))
            cursor = end + 1
            val text = text1.trim()
            if (text.isEmpty()) continue
            if (kind == "TAG" && text.startsWith("</")) depth = (depth - 1).coerceAtLeast(0)
            if (output.isNotEmpty() && output.last() != '\n') output.append('\n')
            repeat(depth * 2) { output.append(' ') }
            output.append(text)
            if (kind == "TAG" && isOpeningContainer(text)) depth++
        }
        if (cursor < payload.length) appendText(output, payload.substring(cursor))
        return output.toString()
    }

    private fun appendText(output: StringBuilder, text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        if (output.isNotEmpty() && output.last() != '\n') output.append('\n')
        output.append(trimmed)
    }

    private fun isOpeningContainer(tag: String): Boolean {
        if (tag.startsWith("</") || tag.endsWith("/>") || tag.startsWith("<!") || tag.startsWith("<?")) return false
        val name = tag.drop(1).takeWhile { it.isLetterOrDigit() || it in "-:_" }.lowercase()
        return name !in VOID_ELEMENTS
    }

    private fun highlightTokens(
        payload: String,
        tokens: List<MarkupToken>,
        theme: BodyRendererTheme,
        output: SpannedTextBuilder,
    ) {
        var cursor = 0
        for ((start1, end1, _, kind) in tokens) {
            val start = start1.coerceAtLeast(cursor)
            if (start > cursor) output.append(payload.substring(cursor, start))
            val end = (end1 + 1).coerceAtMost(payload.length)
            val color = when (kind) {
                "TAG" -> theme.tag
                "COMMENT" -> theme.comment
                "DOCTYPE" -> theme.doctype
                "TEXT", "CDATA" -> theme.text
                else -> null
            }
            if (kind == "TAG") highlightTag(output, payload.substring(start, end), theme)
            else output.append(payload.substring(start, end), color)
            cursor = end
        }
        if (cursor < payload.length) output.append(payload.substring(cursor))
    }

    private fun highlightTag(output: SpannedTextBuilder, tag: String, theme: BodyRendererTheme) {
        var index = 0
        if (tag.startsWith("<") && tag.isNotEmpty()) {
            appendColored(output, "<", theme.separator)
            index++
        }
        if (tag.getOrNull(index) == '/' || tag.getOrNull(index) == '?') {
            appendColored(output, tag[index].toString(), theme.separator)
            index++
        }
        val nameStart = index
        while (index < tag.length && !tag[index].isWhitespace() && tag[index] !in "/>?") index++
        if (index > nameStart) appendColored(output, tag.substring(nameStart, index), theme.tag)
        while (index < tag.length) {
            val character = tag[index]
            when {
                character.isWhitespace() -> {
                    output.append(character.toString())
                    index++
                }
                character == '>' || character == '/' || character == '?' -> {
                    appendColored(output, character.toString(), theme.separator)
                    index++
                }
                character == '=' -> {
                    appendColored(output, "=", theme.separator)
                    index++
                }
                character == '\'' || character == '"' -> {
                    val valueStart = index
                    index++
                    while (index < tag.length && tag[index] != character) index++
                    if (index < tag.length) index++
                    appendColored(output, tag.substring(valueStart, index), theme.attributeValue)
                }
                else -> {
                    val attributeStart = index
                    while (index < tag.length && !tag[index].isWhitespace() && tag[index] !in "=/>?") index++
                    if (index == attributeStart) index++
                    appendColored(output, tag.substring(attributeStart, index), theme.attributeName)
                }
            }
        }
    }

    private fun appendColored(output: SpannedTextBuilder, text: String, color: Color) {
        output.append(text, color)
    }

}
