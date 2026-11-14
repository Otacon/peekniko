package peekniko.internal.bodyrenderer

import javax.swing.JTextPane
import javax.swing.text.DefaultStyledDocument
import javax.swing.text.SimpleAttributeSet
import javax.swing.text.StyleConstants
import javax.swing.text.StyledDocument

internal actual fun defaultSpannedTextBuilderFactory(): SpannedTextBuilderFactory =
    SpannedTextBuilderFactory { SpannedTextBuilderDefault() }

internal class SpannedTextBuilderDefault : SpannedTextBuilder {
    private val platformText: StyledDocument = DefaultStyledDocument()

    override fun append(text: String, color: Color?, isBold: Boolean) {
        if (text.isEmpty()) return
        val offset = platformText.length
        val attributes = if (color != null || isBold) SimpleAttributeSet().apply {
            color?.let { StyleConstants.setForeground(this, it.platformColor) }
            if (isBold) StyleConstants.setBold(this, true)
        } else null
        platformText.insertString(offset, text, attributes)
    }

    override fun append(other: SpannedTextBuilder) {
        val platformOther = other as SpannedTextBuilderDefault
        var sourceOffset = 0
        while (sourceOffset < platformOther.platformText.length) {
            val element = platformOther.platformText.getCharacterElement(sourceOffset)
            val end = minOf(element.endOffset, platformOther.platformText.length)
            val chunk = platformOther.platformText.getText(sourceOffset, end - sourceOffset)
            platformText.insertString(platformText.length, chunk, element.attributes)
            sourceOffset = end
        }
    }

    override fun build(): SpannedText = NativeSpannedText(
        DefaultStyledDocument().also { copyDocument(platformText, it) },
    )
}

internal class NativeSpannedText(val platformText: StyledDocument) : SpannedText

internal val SpannedText.platformText: StyledDocument
    get() = (this as NativeSpannedText).platformText

internal fun JTextPane.setSpannedText(text: SpannedText?) {
    document = (text ?: emptySpannedText()).platformText
}

private fun copyDocument(source: StyledDocument, destination: StyledDocument) {
    var sourceOffset = 0
    while (sourceOffset < source.length) {
        val element = source.getCharacterElement(sourceOffset)
        val end = minOf(element.endOffset, source.length)
        destination.insertString(
            destination.length,
            source.getText(sourceOffset, end - sourceOffset),
            element.attributes,
        )
        sourceOffset = end
    }
}
