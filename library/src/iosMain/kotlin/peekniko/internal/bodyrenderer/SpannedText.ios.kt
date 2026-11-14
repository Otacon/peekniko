package peekniko.internal.bodyrenderer

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSAttributedString
import platform.Foundation.NSMakeRange
import platform.Foundation.NSMutableAttributedString
import platform.Foundation.appendAttributedString
import platform.Foundation.create
import platform.UIKit.NSFontAttributeName
import platform.UIKit.NSForegroundColorAttributeName
import platform.UIKit.UIFont
import platform.UIKit.UITextView

internal actual fun defaultSpannedTextBuilderFactory(): SpannedTextBuilderFactory =
    SpannedTextBuilderFactory { SpannedTextBuilderDefault() }

@OptIn(BetaInteropApi::class, ExperimentalForeignApi::class)
internal class SpannedTextBuilderDefault : SpannedTextBuilder {
    private val platformText = NSMutableAttributedString.create(string = "")
    private val bodyFont = UIFont.systemFontOfSize(14.0)
    private var cursor = 0
    private val textBuilder = StringBuilder()

    override fun append(text: String, color: Color?, isBold: Boolean) {
        if (text.isEmpty()) return
        platformText.replaceCharactersInRange(NSMakeRange(cursor.toULong(), 0uL), text)
        val attributes = mutableMapOf<Any?, Any?>(NSFontAttributeName to if (isBold) UIFont.boldSystemFontOfSize(14.0) else bodyFont)
        color?.let { attributes[NSForegroundColorAttributeName] = it.platformColor }
        platformText.setAttributes(attributes, NSMakeRange(cursor.toULong(), text.length.toULong()))
        cursor += text.length
        textBuilder.append(text)
    }

    override fun append(other: SpannedTextBuilder) {
        val platformOther = other as SpannedTextBuilderDefault
        val otherText = platformOther.textBuilder.toString()
        platformText.appendAttributedString(platformOther.platformText)
        cursor += otherText.length
        textBuilder.append(otherText)
    }

    override fun build(): SpannedText = NativeSpannedText(
        NSAttributedString.create(attributedString = platformText),
    )
}

internal class NativeSpannedText(val platformText: NSAttributedString) : SpannedText

internal val SpannedText.platformText: NSAttributedString
    get() = (this as NativeSpannedText).platformText

internal fun UITextView.setSpannedText(text: SpannedText?) {
    this.attributedText = (text ?: emptySpannedText()).platformText
}
