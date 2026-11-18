package peekniko

import peekniko.internal.bodyrenderer.BodyRendererJson
import peekniko.internal.bodyrenderer.BodyRendererTheme
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

internal class BodyRendererJsonTest : FunSpec({
    lateinit var renderer: BodyRendererJson

    beforeTest {
        renderer = BodyRendererJson()
    }

    context("content type support") {
        test("supports application/json") {
            renderer.supports("application/json") shouldBe true
        }

        test("supports text/json") {
            renderer.supports("text/json") shouldBe true
        }

        test("supports vendor JSON media types") {
            renderer.supports("application/vnd.api+json") shouldBe true
        }

        test("supports media types with parameters") {
            renderer.supports("application/json; charset=utf-8") shouldBe true
        }

        test("trims whitespace around the media type") {
            renderer.supports("  application/json  ") shouldBe true
        }

        test("matches media types case-insensitively") {
            renderer.supports("APPLICATION/JSON") shouldBe true
        }

        test("does not support an unrelated media type") {
            renderer.supports("text/plain") shouldBe false
        }

        test("does not accept a JSON-like suffix without plus") {
            renderer.supports("application/jsonp") shouldBe false
        }
    }

    context("syntax highlighting") {
        test("colors JSON keys, strings, punctuation, and integers with theme roles") {
            val payload = """{"name":"Ada","age":7}"""

            val builder = renderer.syntaxHighlight(BodyRendererTheme.Light, payload, "application/json")
            builder.bbText shouldBe
                    "[c=Light.separator]{[/c]" +
                    "[c=Light.key]\"name\"[/c]" +
                    "[c=Light.separator]:[/c]" +
                    "[c=Light.string]\"Ada\"[/c]" +
                    "[c=Light.separator],[/c]" +
                    "[c=Light.key]\"age\"[/c]" +
                    "[c=Light.separator]:[/c]" +
                    "[c=Light.integer]7[/c]" +
                    "[c=Light.separator]}[/c]"
        }

        test("uses color names from the selected theme") {
            val result = renderer.syntaxHighlight(BodyRendererTheme.Dark, "{\"key\":\"value\"}", "application/json")
            result.bbText shouldBe
                    "[c=Dark.separator]{[/c]" +
                    "[c=Dark.key]\"key\"[/c]" +
                    "[c=Dark.separator]:[/c]" +
                    "[c=Dark.string]\"value\"[/c]" +
                    "[c=Dark.separator]}[/c]"
        }

        test("highlights true and false as booleans") {
            val result = renderer.syntaxHighlight(BodyRendererTheme.Light, "[true,false]", "application/json")

            result.bbText shouldBe
                    "[c=Light.separator]\\[[/c]" +
                    "[c=Light.boolean]true[/c]" +
                    "[c=Light.separator],[/c]" +
                    "[c=Light.boolean]false[/c]" +
                    "[c=Light.separator]\\][/c]"
        }

        test("highlights arrays of objects") {
            val payload = """
                [
                  {"name":"Ada"},
                  {"name":"Grace"}
                ]
            """.trimIndent()

            val result = renderer.syntaxHighlight(BodyRendererTheme.Light, payload, "application/json")

            result.bbText shouldBe """
                [c=Light.separator]\[[/c]
                  [c=Light.separator]{[/c][c=Light.key]"name"[/c][c=Light.separator]:[/c][c=Light.string]"Ada"[/c][c=Light.separator]}[/c][c=Light.separator],[/c]
                  [c=Light.separator]{[/c][c=Light.key]"name"[/c][c=Light.separator]:[/c][c=Light.string]"Grace"[/c][c=Light.separator]}[/c]
                [c=Light.separator]\][/c]
            """.trimIndent()
        }

        test("higlights malformed json") {
            val malformedJson = "{not valid JSON]"

            val result = renderer.syntaxHighlight(BodyRendererTheme.Light, malformedJson, "application/json")
            val output = result.bbText
            output shouldBe "[c=Light.separator]{[/c]not valid JSON[c=Light.separator]\\][/c]"
        }
    }

    context("formatting") {
        test("formats valid JSON") {
            val input = """{"name":"Ada"}"""
            val expected = """
                {
                  "name": "Ada"
                }
            """.trimIndent()

            renderer.format(input, "application/json") shouldBe expected
        }

        test("formats arrays of objects with readable indentation") {
            val input = """[{"name":"Ada"},{"name":"Grace"}]"""
            val expected = """
                [
                  {
                    "name": "Ada"
                  },
                  {
                    "name": "Grace"
                  }
                ]
            """.trimIndent()

            renderer.format(input, "application/json") shouldBe expected
        }

        test("returns null when the payload is not valid JSON") {
            renderer.format("{not valid JSON]", "application/json") shouldBe null
        }
    }
})
