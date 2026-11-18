package peekniko.internal.bodyrenderer

import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.graphics.Typeface
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

internal class SpannedTextPlatformTest : FunSpec({
    test("builds Android spanned text with color and bold style") {
        val builder = platformSpannedTextBuilderFactory().create()
        builder.append("plain ")
        builder.append("bold", Color(255, 0, 0), isBold = true)

        val text = (builder.build() as NativeSpannedText).platformText

        text.toString() shouldBe "plain bold"
        text.getSpanStart(text.getSpans(6, 10, ForegroundColorSpan::class.java).single()) shouldBe 6
        text.getSpanEnd(text.getSpans(6, 10, ForegroundColorSpan::class.java).single()) shouldBe 10
        text.getSpans(6, 10, ForegroundColorSpan::class.java).single().foregroundColor shouldBe
                android.graphics.Color.RED
        text.getSpans(6, 10, StyleSpan::class.java).single().style shouldBe Typeface.BOLD
        text.getSpanFlags(text.getSpans(6, 10, ForegroundColorSpan::class.java).single()) shouldBe
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
    }
})
