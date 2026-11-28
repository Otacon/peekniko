package peekniko.internal.clipboard

// navigator.clipboard is missing outside secure contexts, so the call can throw synchronously. The write itself is
// asynchronous: a later rejection (e.g. no user activation) can't be reported back, so it's only swallowed.
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
