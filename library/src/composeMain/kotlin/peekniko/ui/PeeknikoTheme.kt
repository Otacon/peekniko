package peekniko.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

internal val darkTheme = darkColorScheme(
    primary = White,
    onPrimary = Black,
    primaryContainer = Gray10,
    onPrimaryContainer = White,
    inversePrimary = Black,
    secondary = White,
    onSecondary = Black,
    secondaryContainer = Gray10,
    onSecondaryContainer = White,
    tertiary = White,
    onTertiary = Black,
    tertiaryContainer = Gray10,
    onTertiaryContainer = White,
    background = Black,
    onBackground = White,
    surface = Black,
    onSurface = White,
    surfaceVariant = Gray10,
    onSurfaceVariant = Gray69,
    surfaceTint = White,
    inverseSurface = White,
    inverseOnSurface = Black,
    error = White,
    onError = Black,
    errorContainer = Gray10,
    onErrorContainer = White,
    outline = Gray69,
    outlineVariant = Gray20,
    scrim = Black,
    surfaceBright = Gray10,
    surfaceDim = Black,
    surfaceContainer = Gray10,
    surfaceContainerHigh = Gray10,
    surfaceContainerHighest = Gray10,
    surfaceContainerLow = Gray10,
    surfaceContainerLowest = Black,
)

internal val lightTheme = lightColorScheme(
    primary = Black,
    onPrimary = White,
    primaryContainer = Gray95,
    onPrimaryContainer = Black,
    inversePrimary = White,
    secondary = Black,
    onSecondary = White,
    secondaryContainer = Gray95,
    onSecondaryContainer = Black,
    tertiary = Black,
    onTertiary = White,
    tertiaryContainer = Gray95,
    onTertiaryContainer = Black,
    background = White,
    onBackground = Black,
    surface = White,
    onSurface = Black,
    surfaceVariant = Gray95,
    onSurfaceVariant = Gray40,
    surfaceTint = Black,
    inverseSurface = Black,
    inverseOnSurface = White,
    error = Black,
    onError = White,
    errorContainer = Gray95,
    onErrorContainer = Black,
    outline = Gray40,
    outlineVariant = Gray88,
    scrim = Black,
    surfaceBright = Gray95,
    surfaceDim = White,
    surfaceContainer = Gray95,
    surfaceContainerHigh = Gray95,
    surfaceContainerHighest = Gray95,
    surfaceContainerLow = Gray95,
    surfaceContainerLowest = White,
)

@Composable
internal fun PeeknikoTheme(content: @Composable () -> Unit) {
    val colors = when (isSystemInDarkTheme()) {
        true -> darkTheme
        false -> lightTheme
    }
    MaterialTheme(colorScheme = colors, content = content)
}
