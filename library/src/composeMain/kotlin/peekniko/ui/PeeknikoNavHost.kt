package peekniko.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
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

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.outlineVariant)
    ) {
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
        val detailPane = @Composable { callId: String? ->
            CallDetailsScreen(
                callId = callId,
                showBackButton = !twoPanes,
                onBackPressed = { selectedCallId = null },
            )
        }

        if (twoPanes) {
            Row(modifier = Modifier.fillMaxSize()) {
                Box(modifier = Modifier.width(ListPaneWidth).fillMaxHeight()) { listPane() }
                Spacer(modifier = Modifier.width(1.dp))
                Box(modifier = Modifier.weight(1f).fillMaxHeight()) { detailPane(selectedCallId) }
            }
        } else {
            AnimatedContent(
                targetState = selectedCallId,
                modifier = Modifier.fillMaxSize(),
                contentKey = { it != null },
                transitionSpec = {
                    val toDetail = targetState != null
                    if (toDetail) {
                        slideInHorizontally { width -> width } togetherWith
                                slideOutHorizontally { width -> -width / 3 }
                    } else {
                        slideInHorizontally { width -> -width / 3 } togetherWith
                                slideOutHorizontally { width -> width }
                    }.apply {
                        targetContentZIndex = if (toDetail) {
                            1f
                        } else {
                            0f
                        }
                    }
                },
                label = "PeeknikoListDetail",
            ) { callId ->
                if (callId == null) {
                    listPane()
                } else {
                    detailPane(callId)
                }
            }
        }
    }
}
