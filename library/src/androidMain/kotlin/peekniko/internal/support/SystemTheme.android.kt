package peekniko.internal.support

import android.content.res.Configuration
import android.content.res.Resources

internal actual fun isSystemDarkMode(): Boolean {
    val nightMode = Resources.getSystem().configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
    return nightMode == Configuration.UI_MODE_NIGHT_YES
}
