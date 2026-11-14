package peekniko.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffold
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldDefaults
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.layout.PaneAdaptedValue
import androidx.compose.material3.adaptive.layout.ThreePaneScaffoldDestinationItem
import androidx.compose.material3.adaptive.layout.calculatePaneScaffoldDirectiveWithTwoPanesOnMediumWidth
import androidx.compose.material3.adaptive.layout.calculateThreePaneScaffoldValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
    PeeknikoTheme {
        PeeknikoNavHost(modifier, onClose)
    }
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
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
    val directive = calculatePaneScaffoldDirectiveWithTwoPanesOnMediumWidth(currentWindowAdaptiveInfoV2()).copy(
        horizontalPartitionSpacerSize = 1.dp,
    )
    val value = calculateThreePaneScaffoldValue(
        maxHorizontalPartitions = directive.maxHorizontalPartitions,
        adaptStrategies = ListDetailPaneScaffoldDefaults.adaptStrategies(),
        currentDestination = ThreePaneScaffoldDestinationItem(
            pane = if (selectedCallId == null) {
                ListDetailPaneScaffoldRole.List
            } else {
                ListDetailPaneScaffoldRole.Detail
            },
            contentKey = selectedCallId,
        ),
    )
    ListDetailPaneScaffold(
        directive = directive,
        value = value,
        modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.outlineVariant),
        listPane = {
            AnimatedPane {
                CallListScreen(
                    viewModel = viewModel,
                    onClose = onClose,
                    selectedCallId = selectedCallId,
                    onCallSelected = { selectedCallId = it },
                )
            }
        },
        detailPane = {
            AnimatedPane {
                CallDetailsScreen(
                        callId = selectedCallId,
                        showBackButton = value[ListDetailPaneScaffoldRole.List] != PaneAdaptedValue.Expanded,
                        onBackPressed = { selectedCallId = null },
                )
            }
        },
    )
}
