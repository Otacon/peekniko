package peekniko.internal.bodyrenderer

internal expect class Color(red: Int, green: Int, blue: Int, alpha: Int = 255) {
    val red: Int
    val green: Int
    val blue: Int
    val alpha: Int
}

internal fun String.toColor(): Color {
    val hex = removePrefix("#")
    require(hex.length == 6 || hex.length == 8)
    val offset = if (hex.length == 8) 2 else 0
    val alpha = if (offset == 2) hex.substring(0, 2).toInt(16) else 255
    val red = hex.substring(offset, offset + 2).toInt(16)
    val green = hex.substring(offset + 2, offset + 4).toInt(16)
    val blue = hex.substring(offset + 4, offset + 6).toInt(16)
    return Color(red, green, blue, alpha)
}
