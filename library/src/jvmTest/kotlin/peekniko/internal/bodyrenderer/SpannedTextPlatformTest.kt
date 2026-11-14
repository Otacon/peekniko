package peekniko.internal.bodyrenderer

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import javax.swing.JTextPane
import javax.swing.text.StyleConstants

internal class SpannedTextPlatformTest : FunSpec({
    test("builds styled Swing text") {
        val builder = defaultSpannedTextBuilderFactory().create()
        builder.append("plain ")
        builder.append("bold", Color(255, 0, 0), isBold = true)

        val document = (builder.build() as NativeSpannedText).platformText

        document.getText(0, document.length) shouldBe "plain bold"
        val attributes = document.getCharacterElement(6).attributes
        StyleConstants.getForeground(attributes) shouldBe java.awt.Color(255, 0, 0)
        StyleConstants.isBold(attributes) shouldBe true
    }

    test("uses an empty document when setting null text") {
        val textPane = JTextPane()

        textPane.setSpannedText(null)

        textPane.document.length shouldBe 0
    }
})
