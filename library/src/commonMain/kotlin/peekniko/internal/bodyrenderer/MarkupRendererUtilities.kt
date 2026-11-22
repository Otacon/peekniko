package peekniko.internal.bodyrenderer

import org.antlr.v4.kotlinruntime.Token

internal enum class MarkupKind { Open, Close, Tag, Comment, Cdata, Doctype, Text }

internal data class MarkupToken(
    val start: Int,
    val end: Int,
    val kind: MarkupKind,
)

internal object MarkupRendererUtilities {
    private val VOID_ELEMENTS = setOf(
        "area", "base", "br", "col", "embed", "hr", "img", "input", "link", "meta", "param", "source", "track", "wbr",
    )

    fun format(payload: String, tokens: Sequence<MarkupToken>): String = formatTokens(payload, tokens)

    private fun formatTokens(payload: String, tokens: Sequence<MarkupToken>): String {
        val output = StringBuilder()
        var depth = 0
        var cursor = 0
        for ((start, end, kind) in tokens) {
            if (start > cursor) appendText(output, payload.substring(cursor, start))
            cursor = end + 1
            val text = payload.substring(start, end + 1).trim()
            if (text.isEmpty()) continue
            if (kind == MarkupKind.Tag && text.startsWith("</")) depth = (depth - 1).coerceAtLeast(0)
            if (output.isNotEmpty() && output.last() != '\n') output.append('\n')
            repeat(depth * 2) { output.append(' ') }
            output.append(text)
            if (kind == MarkupKind.Tag && isOpeningContainer(text)) depth++
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

    fun highlight(payload: String, tokens: Sequence<MarkupToken>, theme: BodyRendererTheme, color: ColorSink) {
        var cursor = 0
        for ((tokenStart, tokenEnd, kind) in tokens) {
            val start = tokenStart.coerceAtLeast(cursor)
            val end = (tokenEnd + 1).coerceAtMost(payload.length)
            when (kind) {
                MarkupKind.Tag -> highlightTag(payload, start, end, theme, color)
                MarkupKind.Comment -> color(start, end, theme.comment)
                MarkupKind.Doctype -> color(start, end, theme.doctype)
                MarkupKind.Text, MarkupKind.Cdata -> color(start, end, theme.text)
                MarkupKind.Open, MarkupKind.Close -> Unit
            }
            cursor = end
        }
    }

    private fun highlightTag(payload: String, start: Int, end: Int, theme: BodyRendererTheme, color: ColorSink) {
        var index = start
        if (index < end && payload[index] == '<') {
            color(index, index + 1, theme.separator)
            index++
        }
        if (index < end && (payload[index] == '/' || payload[index] == '?')) {
            color(index, index + 1, theme.separator)
            index++
        }
        val nameStart = index
        while (index < end && !payload[index].isWhitespace() && payload[index] !in "/>?") index++
        if (index > nameStart) color(nameStart, index, theme.tag)
        while (index < end) {
            val character = payload[index]
            when {
                character.isWhitespace() -> index++
                character == '>' || character == '/' || character == '?' || character == '=' -> {
                    color(index, index + 1, theme.separator)
                    index++
                }
                character == '\'' || character == '"' -> {
                    val valueStart = index
                    index++
                    while (index < end && payload[index] != character) index++
                    if (index < end) index++
                    color(valueStart, index, theme.attributeValue)
                }
                else -> {
                    val attributeStart = index
                    while (index < end && !payload[index].isWhitespace() && payload[index] !in "=/>?") index++
                    if (index == attributeStart) index++
                    color(attributeStart, index, theme.attributeName)
                }
            }
        }
    }
}

internal interface MarkupGrammar : TokenGrammar {

    fun kindOf(type: Int): MarkupKind

    fun tokenize(payload: String, tokens: Sequence<Token>): Sequence<MarkupToken> = sequence {
        var tagStart: Int? = null
        for (token in tokens) {
            val kind = kindOf(token.type)
            val start = tagStart
            when {
                kind == MarkupKind.Open -> tagStart = token.startIndex
                start != null && kind == MarkupKind.Close -> {
                    yield(MarkupToken(start, token.endIndex - 1, MarkupKind.Tag))
                    tagStart = null
                }
                start == null -> yield(MarkupToken(token.startIndex, token.endIndex - 1, kind))
            }
        }
        tagStart?.let { yield(MarkupToken(it, payload.lastIndex, MarkupKind.Tag)) }
    }

    override fun highlight(payload: String, tokens: Sequence<Token>, theme: BodyRendererTheme, color: ColorSink) =
        MarkupRendererUtilities.highlight(payload, tokenize(payload, tokens), theme, color)
}
