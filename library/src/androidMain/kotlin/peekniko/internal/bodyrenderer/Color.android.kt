package peekniko.internal.bodyrenderer

import android.graphics.Color as AndroidColor

internal actual class Color actual constructor(
    actual val red: Int,
    actual val green: Int,
    actual val blue: Int,
    actual val alpha: Int,
) {
    init { require(red in 0..255 && green in 0..255 && blue in 0..255 && alpha in 0..255) }

    val platformColor: Int = AndroidColor.argb(alpha, red, green, blue)

    override fun equals(other: Any?): Boolean = other is Color &&
        red == other.red && green == other.green && blue == other.blue && alpha == other.alpha

    override fun hashCode(): Int = (((red * 31) + green) * 31 + blue) * 31 + alpha
}
