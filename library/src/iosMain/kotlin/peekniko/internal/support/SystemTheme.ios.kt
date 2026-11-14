package peekniko.internal.support

import platform.UIKit.UIUserInterfaceStyle
import platform.UIKit.UIScreen

internal actual fun isSystemDarkMode(): Boolean =
    UIScreen.mainScreen.traitCollection.userInterfaceStyle == UIUserInterfaceStyle.UIUserInterfaceStyleDark
