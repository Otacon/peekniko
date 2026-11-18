package peekniko.ui

import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import peekniko.ui.details.PrettyPrintState

@Composable
internal fun PrettyPrintAction(
    prettyPrintState: PrettyPrintState,
    onPrettyPrintClick: (enabled: Boolean) -> Unit,
) {
    when (prettyPrintState) {
        PrettyPrintState.Enabled -> IconButton(onClick = { onPrettyPrintClick(false) }) {
            Icon(imageVector = Icons.PrettyPrint, contentDescription = "Pretty print enabled")
        }

        PrettyPrintState.Disabled -> IconButton(onClick = { onPrettyPrintClick(true) }) {
            Icon(imageVector = Icons.PrettyPrintOff, contentDescription = "Pretty print disabled")
        }

        PrettyPrintState.Unavailable -> Unit
    }
}
