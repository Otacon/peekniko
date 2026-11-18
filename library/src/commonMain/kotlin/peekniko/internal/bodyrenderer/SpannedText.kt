package peekniko.internal.bodyrenderer

import kotlin.concurrent.Volatile

internal interface SpannedText

internal interface SpannedTextBuilder {
    fun append(text: String, color: Color? = null, isBold: Boolean = false)
    fun append(other: SpannedText)
    fun build(): SpannedText
}

internal fun interface SpannedTextBuilderFactory {
    fun create(): SpannedTextBuilder
}

internal expect fun platformSpannedTextBuilderFactory(): SpannedTextBuilderFactory

internal object SpannedTextDefaults {
    @Volatile
    var factory: SpannedTextBuilderFactory = platformSpannedTextBuilderFactory()
}

internal inline fun spannedText(block: SpannedTextBuilder.() -> Unit = {}): SpannedText =
    SpannedTextDefaults.factory.create().apply(block).build()
