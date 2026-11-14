package peekniko.internal.formatting

internal fun Long.formatPayloadSize(): String = when {
    this < 1_000L -> "$this B"
    this < 1_000_000L -> "${this / 1_000L} KB"
    else -> "${this / 1_000_000L} MB"
}
