package peekniko

import peekniko.internal.bodyrenderer.BodyRendererTheme
import peekniko.internal.bodyrenderer.BodyRendererXml
import peekniko.internal.bodyrenderer.MarkupRendererUtilities
import peekniko.internal.bodyrenderer.MarkupToken
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

internal class BodyRendererXmlTest : FunSpec({
    useBBTextSpannedText()

    lateinit var renderer: BodyRendererXml

    beforeTest {
        renderer = BodyRendererXml()
    }

    context("content type support") {
        test("supports application/xml") {
            renderer.supports("application/xml") shouldBe true
        }

        test("supports text/xml") {
            renderer.supports("text/xml") shouldBe true
        }

        test("supports vendor XML media types") {
            renderer.supports("application/atom+xml") shouldBe true
        }

        test("supports XML media types with parameters") {
            renderer.supports("application/xml; charset=utf-8") shouldBe true
        }

        test("does not support a missing media type") {
            renderer.supports(null) shouldBe false
        }

        test("does not support an unrelated media type") {
            renderer.supports("text/plain") shouldBe false
        }
    }

    context("syntax highlighting") {
        test("highlights tag names, attributes, attribute values, and text") {
            val payload = "<item id=\"a\">text</item>"

            val output = renderer.syntaxHighlight(BodyRendererTheme.Light, payload, "application/xml")
            output.bbText shouldBe
                    "[c=Light.separator]<[/c][c=Light.tag]item[/c] " +
                    "[c=Light.attributeName]id[/c][c=Light.separator]=[/c]" +
                    "[c=Light.boolean]\"a\"[/c][c=Light.separator]>[/c]" +
                    "[c=Light.text]text[/c][c=Light.separator]<[/c]" +
                    "[c=Light.separator]/[/c][c=Light.tag]item[/c][c=Light.separator]>[/c]"
        }

        test("uses color roles from the selected theme") {
            val output = renderer.syntaxHighlight(BodyRendererTheme.Dark, "<item>value</item>", "text/xml")

            output.bbText shouldBe
                    "[c=Dark.separator]<[/c][c=Dark.boolean]item[/c][c=Dark.separator]>[/c]" +
                    "[c=Dark.separator]value[/c][c=Dark.separator]<[/c]" +
                    "[c=Dark.separator]/[/c][c=Dark.boolean]item[/c][c=Dark.separator]>[/c]"
        }

        test("highlights XML comments with the comment theme role") {
            val payload = "<root><!--remark--></root>"
            val output = renderer.syntaxHighlight(BodyRendererTheme.Light, payload, "application/xml")

            output.bbText shouldBe
                    "[c=Light.separator]<[/c][c=Light.tag]root[/c][c=Light.separator]>[/c]" +
                    "[c=Light.comment]<!--remark-->[/c]" +
                    "[c=Light.separator]<[/c][c=Light.separator]/[/c][c=Light.tag]root[/c][c=Light.separator]>[/c]"
        }

        test("highlights CDATA as text") {
            val payload = "<root><![CDATA[raw text]]></root>"
            val output = renderer.syntaxHighlight(BodyRendererTheme.Light, payload, "application/xml")

            output.bbText shouldBe
                    "[c=Light.separator]<[/c][c=Light.tag]root[/c][c=Light.separator]>[/c]" +
                    "[c=Light.text]<!\\[CDATA\\[raw text\\]\\]>[/c]" +
                    "[c=Light.separator]<[/c][c=Light.separator]/[/c][c=Light.tag]root[/c][c=Light.separator]>[/c]"
        }

        test("maps a DOCTYPE token to the doctype theme role") {
            val payload = "<!DOCTYPE root>"
            val builder = SpannedTextBuilderBBText()
            MarkupRendererUtilities.syntaxHighlight(
                payload = payload,
                tokens = listOf(MarkupToken(0, payload.lastIndex, payload, "DOCTYPE")),
                theme = BodyRendererTheme.Light,
                output = builder,
            )

            builder.buildText() shouldBe "[c=Light.doctype]<!DOCTYPE root>[/c]"
        }

        test("highlights malformed xml") {
            val malformedXml = "not XML <"

            val output = renderer.syntaxHighlight(BodyRendererTheme.Light, malformedXml, "application/xml")

            output.bbText shouldBe "[c=Light.text]not XML [/c][c=Light.separator]<[/c]"
        }
    }

    context("formatting") {
        test("formats nested XML with readable indentation") {
            val payload = "<root><item>one</item></root>"
            val expected = """
                <root>
                  <item>
                    one
                  </item>
                </root>
            """.trimIndent()

            renderer.format(payload, "application/xml") shouldBe expected
        }

        test("trims surrounding whitespace from text nodes") {
            val payload = "<root>  hello  </root>"

            renderer.format(payload, "application/xml") shouldBe "<root>\n  hello\n</root>"
        }

        test("omits whitespace-only text nodes") {
            renderer.format("<root>   </root>", "application/xml") shouldBe "<root>\n</root>"
        }

        test("trims text skipped between markup tokens") {
            renderer.format("  <!DOCTYPE root>  <root/>  ", "application/xml") shouldBe
                    "<!DOCTYPE root>\n<root/>"
        }
    }
})
