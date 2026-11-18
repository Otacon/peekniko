package peekniko.internal.support

internal actual fun isSystemDarkMode(): Boolean = when {
    System.getProperty("os.name").startsWith("Mac", ignoreCase = true) ->
        runCatching {
            val process = ProcessBuilder("defaults", "read", "-g", "AppleInterfaceStyle")
                .redirectErrorStream(true)
                .start()
            val result = process.inputStream.bufferedReader().use { it.readText() }.trim()
            process.waitFor() == 0 && result.equals("Dark", ignoreCase = true)
        }.getOrDefault(false)

    System.getProperty("os.name").startsWith("Windows", ignoreCase = true) ->
        runCatching {
            val process = ProcessBuilder(
                "reg", "query",
                "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Themes\\Personalize",
                "/v", "AppsUseLightTheme",
            ).redirectErrorStream(true).start()
            val result = process.inputStream.bufferedReader().use { it.readText() }
            process.waitFor() == 0 && result.lineSequence().any {
                it.contains("AppsUseLightTheme") && it.trim().endsWith("0x0")
            }
        }.getOrDefault(false)

    else -> {
        val scheme = runCatching {
            val process = ProcessBuilder("gsettings", "get", "org.gnome.desktop.interface", "color-scheme")
                .redirectErrorStream(true).start()
            val result = process.inputStream.bufferedReader().use { it.readText() }.trim()
            if (process.waitFor() == 0) result else ""
        }.getOrDefault("")
        scheme.contains("dark", ignoreCase = true) ||
                System.getenv("GTK_THEME")?.contains("dark", ignoreCase = true) == true
    }
}
