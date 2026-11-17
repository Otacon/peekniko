package peekniko.internal.bodyrenderer

internal actual class Color actual constructor(
    actual val red: Int,
    actual val green: Int,
    actual val blue: Int,
    actual val alpha: Int,
) {
    init { require(red in 0..255 && green in 0..255 && blue in 0..255 && alpha in 0..255) }

    val platformColor: String = buildString {
        append('#')
        append(red.toHex())
        append(green.toHex())
        append(blue.toHex())
        if (alpha != 255) append(alpha.toHex())
    }

    override fun equals(other: Any?): Boolean = other is Color &&
        red == other.red && green == other.green && blue == other.blue && alpha == other.alpha

    override fun hashCode(): Int = (((red * 31) + green) * 31 + blue) * 31 + alpha
}

private fun Int.toHex(): String = toString(16).padStart(2, '0')
