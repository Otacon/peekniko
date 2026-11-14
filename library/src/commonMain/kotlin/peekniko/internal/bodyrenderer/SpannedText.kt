package peekniko.internal.bodyrenderer

internal interface SpannedText

internal interface SpannedTextBuilder {
    fun append(text: String, color: Color? = null, isBold: Boolean = false)
    fun append(other: SpannedTextBuilder)
    fun build(): SpannedText
}

internal fun interface SpannedTextBuilderFactory {
    fun create(): SpannedTextBuilder
}

internal expect fun defaultSpannedTextBuilderFactory(): SpannedTextBuilderFactory

internal fun spannedText(
    block: SpannedTextBuilder.() -> Unit = {},
): SpannedTextBuilder = defaultSpannedTextBuilderFactory().create().apply(block)

internal fun emptySpannedText() = spannedText().build()
