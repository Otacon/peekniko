package peekniko.internal.clipboard

internal interface Clipboard {
    /** Returns `false` if the platform rejected the text, e.g. because it is too large. */
    fun copy(text: String): Boolean
}

internal expect fun createClipboard(): Clipboard
