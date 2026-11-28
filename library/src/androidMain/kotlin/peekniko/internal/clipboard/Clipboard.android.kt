package peekniko.internal.clipboard

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import peekniko.Peekniko
import peekniko.context

internal actual fun createClipboard(): Clipboard = object : Clipboard {
    override fun copy(text: String): Boolean = try {
        val clipboard = Peekniko.context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val plainText = ClipData.newPlainText("Peekniko", text)
        // Throws when the clip doesn't fit in the ~1 MB Binder transaction buffer.
        clipboard.setPrimaryClip(plainText)
        true
    } catch (_: Exception) {
        false
    }
}
