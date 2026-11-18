package peekniko.ui

import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable

@Composable
internal fun PrettyPrintAction(isPrettyPrintEnabled: Boolean, onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(
            imageVector = if (isPrettyPrintEnabled) Icons.PrettyPrint else Icons.PrettyPrintOff,
            contentDescription = if (isPrettyPrintEnabled) "Pretty print enabled" else "Pretty print disabled",
        )
    }
}
