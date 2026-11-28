package peekniko.internal.clipboard

import java.awt.Toolkit
import java.awt.datatransfer.StringSelection

internal actual fun createClipboard(): Clipboard = object : Clipboard {
    // Throws HeadlessException without a display, IllegalStateException while another app holds the clipboard.
    override fun copy(text: String): Boolean = try {
        val selection = StringSelection(text)
        Toolkit.getDefaultToolkit().systemClipboard.setContents(selection, selection)
        true
    } catch (_: Exception) {
        false
    }
}
