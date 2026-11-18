package peekniko.ui

import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf

@Stable
internal class TopBarActions {
    var content: (@Composable RowScope.() -> Unit)? by mutableStateOf(null)
        private set

    internal fun set(actions: @Composable RowScope.() -> Unit) {
        content = actions
    }

    internal fun clear(actions: @Composable RowScope.() -> Unit) {
        if (content === actions) content = null
    }
}

internal val LocalTopBarActions = staticCompositionLocalOf<TopBarActions> {
    error("No TopBarActions provided")
}

@Composable
internal fun TopBarActionsEffect(actions: @Composable RowScope.() -> Unit) {
    val topBarActions = LocalTopBarActions.current
    val currentActions by rememberUpdatedState(actions)
    DisposableEffect(topBarActions) {
        val content: @Composable RowScope.() -> Unit = { currentActions() }
        topBarActions.set(content)
        onDispose { topBarActions.clear(content) }
    }
}
