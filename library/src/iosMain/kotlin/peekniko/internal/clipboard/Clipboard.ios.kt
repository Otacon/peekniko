package peekniko.internal.clipboard

import platform.UIKit.UIPasteboard

internal actual fun createClipboard(): Clipboard = object : Clipboard {
    override fun copy(text: String): Boolean = try {
        UIPasteboard.generalPasteboard.string = text
        true
    } catch (_: Exception) {
        false
    }
}
