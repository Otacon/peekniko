package peekniko

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import peekniko.internal.bodyrenderer.BodyRendererComposite
import peekniko.internal.bodyrenderer.BodyRendererJson
import peekniko.internal.bodyrenderer.BodyRendererTheme
import peekniko.internal.bodyrenderer.BodyRendererXml

internal class BodyRendererCompositeTest : FunSpec({
    useBBTextSpannedText()

    val renderer = BodyRendererComposite(renderers = listOf(BodyRendererJson(), BodyRendererXml()))

    context("content type support") {
        test("supports a content type any renderer supports") {
            renderer.supports("application/json") shouldBe true
            renderer.supports("application/xml") shouldBe true
        }

        test("does not support a content type no renderer supports") {
            renderer.supports("text/plain") shouldBe false
        }
    }

    context("formatting") {
        test("delegates to the renderer supporting the content type") {
            renderer.format("""{"a":1}""", "application/json") shouldBe "{\n  \"a\": 1\n}"
        }

        test("returns null when no renderer matches") {
            renderer.format("""{"a":1}""", "text/plain") shouldBe null
        }
    }

    context("syntax highlighting") {
        test("delegates to the renderer supporting the content type") {
            renderer.syntaxHighlight(BodyRendererTheme.Light, "<a/>", "application/xml").bbText shouldBe
                    "[c=Light.separator]<[/c][c=Light.tag]a[/c][c=Light.separator]/[/c][c=Light.separator]>[/c]"
        }

        test("returns null when no renderer matches") {
            renderer.syntaxHighlight(BodyRendererTheme.Light, "[plain]", "text/plain") shouldBe null
        }
    }
})
