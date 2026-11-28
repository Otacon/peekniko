package peekniko.internal.clipboard

@OptIn(ExperimentalWasmJsInterop::class)
@JsFun(
    """(text) => {
        try {
            navigator.clipboard.writeText(text).catch(() => {});
            return true;
        } catch (e) {
            return false;
        }
    }"""
)
private external fun writeClipboardText(text: String): Boolean

internal actual fun createClipboard(): Clipboard = object : Clipboard {
    override fun copy(text: String): Boolean = writeClipboardText(text)
}
