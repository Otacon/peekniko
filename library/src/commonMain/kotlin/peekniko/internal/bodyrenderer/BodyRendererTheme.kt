package peekniko.internal.bodyrenderer

import peekniko.internal.support.isSystemDarkMode

internal sealed interface BodyRendererTheme {
    val key: Color
    val string: Color
    val integer: Color
    val number: Color
    val boolean: Color
    val nullValue: Color
    val separator: Color
    val tag: Color
    val attributeName: Color
    val attributeValue: Color
    val comment: Color
    val doctype: Color
    val text: Color

    data object Dark : BodyRendererTheme {
        override val key = "#9CDCFE".toColor()
        override val string = "#CE9178".toColor()
        override val integer = "#B5CEA8".toColor()
        override val number = "#B5CEA8".toColor()
        override val boolean = "#569CD6".toColor()
        override val nullValue = "#569CD6".toColor()
        override val separator = "#D4D4D4".toColor()
        override val tag = "#569CD6".toColor()
        override val attributeName = "#9CDCFE".toColor()
        override val attributeValue = "#CE9178".toColor()
        override val comment = "#6A9955".toColor()
        override val doctype = "#C586C0".toColor()
        override val text = "#D4D4D4".toColor()
    }

    data object Light : BodyRendererTheme {
        override val key = "#0451A5".toColor()
        override val string = "#A31515".toColor()
        override val integer = "#098658".toColor()
        override val number = "#098658".toColor()
        override val boolean = "#0000FF".toColor()
        override val nullValue = "#0000FF".toColor()
        override val separator = "#333333".toColor()
        override val tag = "#800000".toColor()
        override val attributeName = "#FF0000".toColor()
        override val attributeValue = "#0000FF".toColor()
        override val comment = "#008000".toColor()
        override val doctype = "#800080".toColor()
        override val text = "#202020".toColor()
    }
}

internal class BodyRendererThemeProvider(
    private val resolveTheme: () -> BodyRendererTheme = {
        if (isSystemDarkMode()) BodyRendererTheme.Dark else BodyRendererTheme.Light
    },
) {
    val theme: BodyRendererTheme
        get() = resolveTheme()
}
