package peekniko.internal.bodyrenderer

internal class HtmlSpannedTextBuilder : SpannedTextBuilder {
    private val builder = StringBuilder()

    override fun append(text: String, color: Color?, isBold: Boolean) {
        when {
            color != null -> {
                builder.append("<span style=\"color:").append(color.platformColor).append(';')
                if (isBold) builder.append("font-weight:bold;")
                builder.append("\">")
            }
            isBold -> builder.append("<strong>")
        }
        for (character in text) {
            when (character) {
                '&' -> builder.append("&amp;")
                '<' -> builder.append("&lt;")
                '>' -> builder.append("&gt;")
                '"' -> builder.append("&quot;")
                else -> builder.append(character)
            }
        }
        when {
            color != null -> builder.append("</span>")
            isBold -> builder.append("</strong>")
        }
    }

    override fun append(other: SpannedTextBuilder) {
        builder.append((other as HtmlSpannedTextBuilder).builder)
    }

    override fun build(): HtmlSpannedText = HtmlSpannedText(builder.toString())
}

internal class HtmlSpannedText(val html: String) : SpannedText
