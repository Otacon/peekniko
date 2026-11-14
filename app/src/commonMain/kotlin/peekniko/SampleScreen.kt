package peekniko

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
internal fun SampleScreen(
    viewModelFactory: ViewModelProvider.Factory,
    onOpenPeekniko: (() -> Unit)?,
) {
    val viewModel: SampleViewModel = viewModel(factory = viewModelFactory)
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
@OptIn(ExperimentalMaterial3Api::class)
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
