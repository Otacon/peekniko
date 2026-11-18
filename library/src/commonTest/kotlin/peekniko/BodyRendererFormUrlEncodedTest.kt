package peekniko

import peekniko.internal.bodyrenderer.BodyRendererFormUrlEncoded
import peekniko.internal.bodyrenderer.BodyRendererTheme
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

internal class BodyRendererFormUrlEncodedTest : FunSpec({
    useBBTextSpannedText()

    lateinit var renderer: BodyRendererFormUrlEncoded

    beforeTest {
        renderer = BodyRendererFormUrlEncoded()
    }

    context("content type support") {
        test("supports application/x-www-form-urlencoded") {
            renderer.supports("application/x-www-form-urlencoded") shouldBe true
        }

        test("supports content type parameters") {
            renderer.supports("application/x-www-form-urlencoded; charset=utf-8") shouldBe true
        }

        test("matches the content type case-insensitively") {
            renderer.supports("APPLICATION/X-WWW-FORM-URLENCODED") shouldBe true
        }

        test("does not support other content types") {
            renderer.supports("text/plain") shouldBe false
        }
    }

    context("syntax highlighting") {
        test("highlights a key, separator, and value with theme roles") {
            val output = renderer.syntaxHighlight(BodyRendererTheme.Light, "k=v", "application/x-www-form-urlencoded")

            output.bbText shouldBe
                    "[c=Light.key]k[/c][c=Light.separator]=[/c][c=Light.boolean]v[/c]"
        }

        test("highlights field separators and resets value highlighting for the next key") {
            val output = renderer.syntaxHighlight(
                BodyRendererTheme.Dark,
                "a=1&b=2",
                "application/x-www-form-urlencoded",
            )

            output.bbText shouldBe
                    "[c=Dark.key]a[/c][c=Dark.separator]=[/c][c=Dark.string]1[/c]" +
                    "[c=Dark.separator]&[/c][c=Dark.key]b[/c][c=Dark.separator]=[/c][c=Dark.string]2[/c]"
        }
    }

    context("formatting") {
        test("formats equals, ampersands, and plus signs") {
            renderer.format("name=Ada&city=New+York", "application/x-www-form-urlencoded") shouldBe
                    "name = Ada\ncity = New York"
        }
    }
})
