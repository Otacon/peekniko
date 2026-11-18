package peekniko.internal.bodyrenderer

import peekniko.internal.json.generated.JSONBaseVisitor
import peekniko.internal.json.generated.JSONLexer
import peekniko.internal.json.generated.JSONLexer.Tokens
import peekniko.internal.json.generated.JSONParser
import org.antlr.v4.kotlinruntime.CharStreams
import org.antlr.v4.kotlinruntime.CommonTokenStream
import org.antlr.v4.kotlinruntime.Token

internal class BodyRendererJson : BodyRenderer {
    override fun supports(contentType: String): Boolean {
        val mediaType = contentType.substringBefore(';').trim().lowercase()
        return mediaType == "application/json" || mediaType == "text/json" || mediaType.endsWith("+json")
    }

    override fun format(payload: String, contentType: String): String? = runCatching {
        with(FormatVisitor()) {
            visit(parse(payload))
            formattedText()
        }
    }.getOrNull()

    override fun syntaxHighlight(theme: BodyRendererTheme, payload: String, contentType: String): SpannedText? =
        runCatching { highlight(payload, theme) }.getOrNull()

    private fun parse(payload: String): JSONParser.JsonContext {
        val lexer = JSONLexer(CharStreams.fromString(payload))
        val tokens = CommonTokenStream(lexer)
        val parser = JSONParser(tokens)
        val tree = parser.json()
        require(parser.numberOfSyntaxErrors == 0) { "Payload is not valid JSON" }
        return tree
    }

    private fun highlight(payload: String, theme: BodyRendererTheme): SpannedText = spannedText {
        val lexer = JSONLexer(CharStreams.fromString(payload))
        lexer.removeErrorListeners()
        var token = lexer.nextToken()
        var cursor = 0
        while (token.type != Token.EOF) {
            val next = lexer.nextToken()
            val color = when (token.type) {
                Tokens.STRING -> if (next.type == Tokens.COLON) theme.key else theme.string
                Tokens.NUMBER -> {
                    val text = token.text.orEmpty()
                    if (text.contains('.') || text.contains('e', ignoreCase = true)) theme.number else theme.integer
                }

                Tokens.BOOLEAN -> theme.boolean
                Tokens.NULL -> theme.nullValue
                else -> theme.separator
            }
            append(payload.substring(cursor, token.startIndex))
            cursor = token.stopIndex + 1
            append(payload.substring(token.startIndex, cursor), color)
            token = next
        }
        append(payload.substring(cursor))
    }

}

private class FormatVisitor : JSONBaseVisitor<Unit>() {
    private val output = StringBuilder()
    private var depth = 0
    private var lineStart = true

    override fun defaultResult() = Unit

    fun formattedText(): String = output.toString()

    override fun visitJson(ctx: JSONParser.JsonContext) {
        visit(ctx.value())
    }

    override fun visitObj(ctx: JSONParser.ObjContext) {
        appendIndented("{")
        depth++
        val pairs = ctx.pair()
        if (pairs.isEmpty()) {
            depth--
            appendIndented("}")
            return
        }
        newline()
        pairs.forEachIndexed { index, pair ->
            if (index > 0) {
                output.append(',')
                newline()
            }
            indent()
            output.append(pair.STRING().text).append(": ")
            lineStart = false
            visit(pair.value())
        }
        depth--
        newline()
        indent()
        output.append('}')
        lineStart = false
    }

    override fun visitArr(ctx: JSONParser.ArrContext) {
        appendIndented("[")
        depth++
        val values = ctx.value()
        if (values.isEmpty()) {
            depth--
            appendIndented("]")
            return
        }
        newline()
        values.forEachIndexed { index, value ->
            if (index > 0) {
                output.append(',')
                newline()
            }
            indent()
            lineStart = false
            visit(value)
        }
        depth--
        newline()
        indent()
        output.append(']')
        lineStart = false
    }

    override fun visitValue(ctx: JSONParser.ValueContext) {
        val nested = ctx.obj() ?: ctx.arr()
        if (nested != null) visit(nested)
        else output.append(ctx.text)
    }

    private fun appendIndented(text: String) {
        if (lineStart) indent()
        output.append(text)
        lineStart = false
    }

    private fun indent() = repeat(depth * 2) { output.append(' ') }
    private fun newline() {
        output.append('\n')
        lineStart = true
    }
}
