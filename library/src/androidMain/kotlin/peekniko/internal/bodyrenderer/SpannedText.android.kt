package peekniko.internal.bodyrenderer

import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.SpannedString as AndroidSpannedString
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.graphics.Typeface
import android.widget.TextView

internal actual fun platformSpannedTextBuilderFactory(): SpannedTextBuilderFactory =
    SpannedTextBuilderFactory { SpannedTextBuilderDefault() }

internal class SpannedTextBuilderDefault : SpannedTextBuilder {
    private val platformText = SpannableStringBuilder()

    override fun append(text: String, color: Color?, isBold: Boolean) {
        if (text.isEmpty()) return
        val start = platformText.length
        platformText.append(text)
        color?.let {
            platformText.setSpan(ForegroundColorSpan(it.platformColor), start, platformText.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        if (isBold) {
            platformText.setSpan(StyleSpan(Typeface.BOLD), start, platformText.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
    }

    override fun append(other: SpannedText) {
        platformText.append(other.platformText)
    }

    override fun build(): SpannedText = NativeSpannedText(AndroidSpannedString(platformText))
}

internal class NativeSpannedText(val platformText: AndroidSpannedString) : SpannedText

internal val SpannedText.platformText: AndroidSpannedString
    get() = (this as NativeSpannedText).platformText

internal fun TextView.setSpannedText(text: SpannedText?) {
    this.text = (text ?: spannedText()).platformText
}
