package peekniko

import peekniko.internal.bodyrenderer.BodyRendererPlainText
import peekniko.internal.bodyrenderer.BodyRendererTheme
import peekniko.ui.details.BodyContentType
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

internal class BodyRendererPlainTextTest : FunSpec({

    lateinit var renderer: BodyRendererPlainText

    beforeTest {
        renderer = BodyRendererPlainText { SpannedTextBuilderBBText() }
    }

    context("renderer metadata") {
        test("declares plain text as its content type") {
            renderer.contentType shouldBe BodyContentType.PLAIN_TEXT
        }
    }

    context("content type support") {
        test("supports missing content types") {
            renderer.supports(null) shouldBe true
        }

        test("supports arbitrary content types as a fallback") {
            renderer.supports("application/json; charset=utf-8") shouldBe true
        }
    }

    context("formatting") {
        test("preserves the payload") {
            renderer.format("plain text", "text/plain") shouldBe "plain text"
        }

        test("preserves whitespace and Unicode") {
            renderer.format(
                "  first line\n\t second line\n雪 ☃  ",
                null
            ) shouldBe "  first line\n\t second line\n雪 ☃  "
        }
    }

    context("syntax highlighting") {
        test("writes the payload to the injected BBText builder") {
            val payload = "[plain text]\n雪 ☃"
            val builder = renderer.syntaxHighlight(BodyRendererTheme.Light, payload, "text/plain")
            val actual = (builder as SpannedTextBuilderBBText).buildText()
            actual shouldBe """
                \[plain text\]
                雪 ☃
            """.trimIndent()
        }
    }
})
