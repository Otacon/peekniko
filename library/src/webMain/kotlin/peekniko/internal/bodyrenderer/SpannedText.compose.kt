package peekniko.internal.bodyrenderer

import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight

internal class ComposeSpannedTextBuilder : SpannedTextBuilder {
    private val builder = AnnotatedString.Builder()

    override fun append(text: String, color: Color?, isBold: Boolean) {
        val start = builder.length
        builder.append(text)
        if (text.isNotEmpty() && (color != null || isBold)) {
            builder.addStyle(
                SpanStyle(
                    color = color?.let { ComposeColor(it.red, it.green, it.blue, it.alpha) }
                        ?: ComposeColor.Unspecified,
                    fontWeight = if (isBold) FontWeight.Bold else null,
                ),
                start,
                builder.length,
            )
        }
    }

    override fun append(other: SpannedTextBuilder) {
        builder.append((other as ComposeSpannedTextBuilder).builder.toAnnotatedString())
    }

    override fun build(): ComposeSpannedText = ComposeSpannedText(builder.toAnnotatedString())
}

internal class ComposeSpannedText(val annotatedString: AnnotatedString) : SpannedText

internal fun SpannedText.toAnnotatedString(): AnnotatedString = (this as ComposeSpannedText).annotatedString
