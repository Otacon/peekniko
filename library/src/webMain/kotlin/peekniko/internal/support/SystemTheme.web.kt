package peekniko.internal.support

@OptIn(ExperimentalWasmJsInterop::class)
@JsFun("() => window.matchMedia('(prefers-color-scheme: dark)').matches")
private external fun prefersDarkColorScheme(): Boolean

internal actual fun isSystemDarkMode(): Boolean = prefersDarkColorScheme()
