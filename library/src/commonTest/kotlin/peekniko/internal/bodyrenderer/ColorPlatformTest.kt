package peekniko.internal.bodyrenderer

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

internal class ColorPlatformTest : FunSpec({
    test("parses RGB hex colors with opaque alpha") {
        val color = "#123456".toColor()

        color.red shouldBe 0x12
        color.green shouldBe 0x34
        color.blue shouldBe 0x56
        color.alpha shouldBe 255
    }

    test("parses ARGB hex colors") {
        val color = "#80123456".toColor()

        color.alpha shouldBe 0x80
        color.red shouldBe 0x12
        color.green shouldBe 0x34
        color.blue shouldBe 0x56
    }

    test("compares colors by their components") {
        val first = Color(12, 34, 56, 78)
        val second = Color(12, 34, 56, 78)

        (first == second) shouldBe true
        first.hashCode() shouldBe second.hashCode()
    }

    test("rejects out-of-range color components") {
        shouldThrow<IllegalArgumentException> { Color(256, 0, 0) }
    }

    test("rejects unsupported hex lengths") {
        shouldThrow<IllegalArgumentException> { "#12345".toColor() }
    }
})
