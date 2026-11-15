package peekniko.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import peekniko.Peekniko
import peekniko.ui.networkcalls.PeeknikoViewModel

@Composable
public fun Peekniko.Screen(
    modifier: Modifier = Modifier,
    onClose: () -> Unit = {},
) {
    LaunchedEffect(Unit) { Peekniko.notifications.requestPermission() }
    PeeknikoTheme {
        PeeknikoNavHost(modifier, onClose)
    }
}

private val TwoPaneMinWidth = 600.dp
private val ListPaneWidth = 360.dp

@Composable
internal fun PeeknikoNavHost(
    modifier: Modifier = Modifier,
    onClose: () -> Unit,
) {
    val viewModel = remember { PeeknikoViewModel(Peekniko.networkRepository) }
    DisposableEffect(viewModel) {
        onDispose { viewModel.clear() }
    }
    var selectedCallId by remember { mutableStateOf<String?>(null) }
    val stateHolder = rememberSaveableStateHolder()

    BoxWithConstraints(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.outlineVariant)) {
        val twoPanes = maxWidth >= TwoPaneMinWidth
        val listPane = @Composable {
            stateHolder.SaveableStateProvider("list") {
                CallListScreen(
                    viewModel = viewModel,
                    onClose = onClose,
                    selectedCallId = selectedCallId,
                    onCallSelected = { selectedCallId = it },
                )
            }
        }
        val detailPane = @Composable {
            CallDetailsScreen(
                callId = selectedCallId,
                showBackButton = !twoPanes,
                onBackPressed = { selectedCallId = null },
            )
        }

        if (twoPanes) {
            Row(modifier = Modifier.fillMaxSize()) {
                Box(modifier = Modifier.width(ListPaneWidth).fillMaxHeight()) { listPane() }
                Spacer(modifier = Modifier.width(1.dp))
                Box(modifier = Modifier.weight(1f).fillMaxHeight()) { detailPane() }
            }
        } else if (selectedCallId == null) {
            listPane()
        } else {
            detailPane()
        }
    }
}
