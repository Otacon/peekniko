package peekniko.internal.bodyrenderer

import java.awt.Color as AwtColor

internal actual class Color actual constructor(
    actual val red: Int,
    actual val green: Int,
    actual val blue: Int,
    actual val alpha: Int,
) {
    init { require(red in 0..255 && green in 0..255 && blue in 0..255 && alpha in 0..255) }

    val platformColor: AwtColor = AwtColor(red, green, blue, alpha)

    override fun equals(other: Any?): Boolean = other is Color &&
        red == other.red && green == other.green && blue == other.blue && alpha == other.alpha

    override fun hashCode(): Int = (((red * 31) + green) * 31 + blue) * 31 + alpha
}
