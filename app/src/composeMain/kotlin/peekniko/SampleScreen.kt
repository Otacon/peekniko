package peekniko

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
internal fun SampleScreen(
    viewModel: SampleViewModel,
    onOpenPeekniko: (() -> Unit)?,
) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                SampleEvent.OpenPeekniko -> onOpenPeekniko?.invoke()
            }
        }
    }

    SampleScreenContent(
        state = state,
        onLocalServerToggled = viewModel::onLocalServerToggled,
        onOpenPeekniko = viewModel::onOpenPeeknikoClick,
        onRunOkHttp = viewModel::onRunOkHttp,
        onRunKtor = viewModel::onRunKtor,
        onRunUrlSession = viewModel::onRunUrlSession,
    )
}

@Composable
internal fun SampleScreenContent(
    state: SampleUiState,
    onLocalServerToggled: (Boolean) -> Unit,
    onOpenPeekniko: () -> Unit,
    onRunOkHttp: () -> Unit,
    onRunKtor: () -> Unit,
    onRunUrlSession: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Sample Peekniko app") })
        },
    ) { contentPadding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .padding(16.dp)
        ) {
            Text(
                modifier = Modifier.weight(1.0f),
                text = state.status
            )
            if (state.showLocalServerToggle) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        modifier = Modifier.weight(1.0f),
                        text = "Use local server"
                    )
                    Switch(
                        checked = state.isLocalServerEnabled,
                        onCheckedChange = onLocalServerToggled,
                        enabled = !state.isLoading,
                    )
                }
            }
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = onOpenPeekniko
            ) {
                Text("Open Peekniko")
            }
            if (state.showOkHttpButton) {
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.isLoading,
                    onClick = onRunOkHttp
                ) {
                    Text("Run OkHttp requests")
                }
            }
            Button(
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isLoading,
                onClick = onRunKtor
            ) {
                Text("Run Ktor requests")
            }
            if (state.showUrlSessionButton) {
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.isLoading,
                    onClick = onRunUrlSession
                ) {
                    Text("Run URLSession requests")
                }
            }
        }
    }
}
