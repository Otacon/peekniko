package peekniko

import peekniko.internal.bodyrenderer.BodyRendererHtml
import peekniko.internal.bodyrenderer.BodyRendererTheme
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

internal class BodyRendererHtmlTest : FunSpec({
    useBBTextSpannedText()

    lateinit var renderer: BodyRendererHtml

    beforeTest {
        renderer = BodyRendererHtml()
    }

    context("content type support") {
        test("supports text/html") {
            renderer.supports("text/html") shouldBe true
        }

        test("supports application/html") {
            renderer.supports("application/html") shouldBe true
        }

        test("supports XHTML") {
            renderer.supports("application/xhtml+xml") shouldBe true
        }

        test("supports parameters on an HTML media type") {
            renderer.supports("text/html; charset=utf-8") shouldBe true
        }

        test("trims and normalizes the media type") {
            renderer.supports("  TEXT/HTML  ") shouldBe true
        }

        test("does not support unrelated media types") {
            renderer.supports("text/plain") shouldBe false
        }
    }

    context("syntax highlighting") {
        test("highlights tag names, attributes, values, and text") {
            val payload = "<p class=\"x\">Hello</p>"

            val output = renderer.syntaxHighlight(BodyRendererTheme.Light, payload, "text/html")

            output.bbText shouldBe
                    "[c=Light.separator]<[/c][c=Light.tag]p[/c] " +
                    "[c=Light.attributeName]class[/c][c=Light.separator]=[/c]" +
                    "[c=Light.boolean]\"x\"[/c][c=Light.separator]>[/c]" +
                    "[c=Light.text]Hello[/c][c=Light.separator]<[/c]" +
                    "[c=Light.separator]/[/c][c=Light.tag]p[/c][c=Light.separator]>[/c]"
        }

        test("highlights comments") {
            val output = renderer.syntaxHighlight(BodyRendererTheme.Dark, "<div><!--note--></div>", "text/html")

            output.bbText shouldBe
                    "[c=Dark.separator]<[/c][c=Dark.boolean]div[/c][c=Dark.separator]>[/c]" +
                    "[c=Dark.comment]<!--note-->[/c]" +
                    "[c=Dark.separator]<[/c][c=Dark.separator]/[/c][c=Dark.boolean]div[/c][c=Dark.separator]>[/c]"
        }

        test("highlights the doctype with the doctype theme role") {
            val output = renderer.syntaxHighlight(BodyRendererTheme.Light, "<!DOCTYPE html><html></html>", "text/html")

            output.bbText shouldBe
                    "[c=Light.doctype]<!DOCTYPE html>[/c]" +
                    "[c=Light.separator]<[/c][c=Light.tag]html[/c][c=Light.separator]>[/c]" +
                    "[c=Light.separator]<[/c][c=Light.separator]/[/c]" +
                    "[c=Light.tag]html[/c][c=Light.separator]>[/c]"
        }
    }

    context("formatting") {
        test("formats nested HTML with readable indentation") {
            val payload = "<div><p>Hello</p></div>"
            val expected = """
                <div>
                  <p>
                    Hello
                  </p>
                </div>
            """.trimIndent()

            renderer.format(payload, "text/html") shouldBe expected
        }

        test("omits whitespace-only text nodes") {
            renderer.format("<div>   </div>", "text/html") shouldBe "<div>\n</div>"
        }
    }
})
