package peekniko.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import peekniko.Peekniko
import peekniko.internal.bodyrenderer.SpannedText
import peekniko.internal.bodyrenderer.toAnnotatedString

@Composable
internal fun SummaryTab(callId: String, modifier: Modifier = Modifier) {
    val viewModel = remember(callId) { Peekniko.createSummaryViewModel(callId) }
    DisposableEffect(viewModel) { onDispose { viewModel.clear() } }
    val state by viewModel.state.collectAsState()

    Column(modifier = modifier.verticalScroll(rememberScrollState()).padding(bottom = 16.dp)) {
        SummarySection("General") { SelectableText(state.general) }
        SummarySection("Request headers") { SelectableText(state.requestHeaders) }
        SummarySection("Response headers") { SelectableText(state.responseHeaders) }
    }
}

@Composable
private fun SummarySection(title: String, content: @Composable () -> Unit) {
    Column {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surfaceVariant,
        ) {
            Text(
                title,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                style = MaterialTheme.typography.titleLarge,
            )
        }
        content()
    }
}

@Composable
private fun SelectableText(text: SpannedText) {
    SelectionContainer {
        Text(
            text.toAnnotatedString(),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            fontFamily = FontFamily.Monospace,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}
