package peekniko.internal.clipboard

internal interface Clipboard {
    fun copy(text: String): Boolean
}

internal expect fun createClipboard(): Clipboard
