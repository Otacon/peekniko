package peekniko.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import peekniko.Peekniko
import peekniko.ui.details.NetworkCallDetailViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CallDetailsScreen(
    callId: String?,
    showBackButton: Boolean,
    onBackPressed: () -> Unit,
) {
    val viewModel = remember(callId) { Peekniko.createNetworkCallDetailViewModel(callId) }
    DisposableEffect(viewModel) {
        onDispose { viewModel.clear() }
    }
    val state by viewModel.state.collectAsState()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.title) },
                navigationIcon = {
                    if (showBackButton) {
                        IconButton(onClick = onBackPressed) {
                            Icon(
                                imageVector = Icons.ArrowBack,
                                contentDescription = "Back",
                            )
                        }
                    }
                },
            )
        },
    ) { contentPadding ->
        CallDetailsContent(
            state = state,
            modifier = Modifier.padding(contentPadding),
        )
    }
}

@Composable
private fun CallDetailsContent(
    state: NetworkCallDetailViewModel.State,
    modifier: Modifier = Modifier
) {
    when {
        state.isLoading -> {
            Box(
                modifier = modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(modifier = modifier.fillMaxWidth())
            }
        }

        state.transaction != null -> {
            CallDetailsTabView(
                callId = state.transaction.id,
                modifier = modifier,
            )
        }

        else -> {
            Box(
                modifier = modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(state.emptyMessage)
            }
        }
    }
}

@Composable
private fun CallDetailsTabView(
    callId: String,
    modifier: Modifier = Modifier,
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Headers", "Request", "Response")

    Column(modifier = modifier.fillMaxSize()) {
        PrimaryTabRow(selectedTabIndex = selectedTab) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title) },
                )
            }
        }
        when (selectedTab) {
            0 -> SummaryTab(callId = callId, modifier = Modifier.weight(1f))
            1 -> RequestTab(callId = callId, modifier = Modifier.weight(1f))
            else -> ResponseTab(callId = callId, modifier = Modifier.weight(1f))
        }
    }
}
