package peekniko.internal.bodyrenderer

import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.text.font.FontWeight
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

internal class SpannedTextPlatformTest : FunSpec({
    test("builds styled Compose text") {
        val builder = platformSpannedTextBuilderFactory().create()
        builder.append("plain ")
        builder.append("bold", Color(255, 0, 0), isBold = true)

        val text = builder.build().toAnnotatedString()

        text.text shouldBe "plain bold"
        val style = text.spanStyles.single()
        style.start shouldBe 6
        style.end shouldBe 10
        style.item.color shouldBe ComposeColor.Red
        style.item.fontWeight shouldBe FontWeight.Bold
    }

    test("copies styles when appending another builder") {
        val inner = platformSpannedTextBuilderFactory().create()
        inner.append("red", Color(255, 0, 0))
        val outer = platformSpannedTextBuilderFactory().create()
        outer.append("> ")
        outer.append(inner.build())

        val text = outer.build().toAnnotatedString()

        text.text shouldBe "> red"
        text.spanStyles.single().start shouldBe 2
    }
})
