package peekniko.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
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
import peekniko.internal.bodyrenderer.emptySpannedText
import peekniko.internal.bodyrenderer.toAnnotatedString

@Composable
internal fun ResponseTab(callId: String, modifier: Modifier = Modifier) {
    val viewModel = remember(callId) { Peekniko.createResponseViewModel(callId) }
    DisposableEffect(viewModel) { onDispose { viewModel.clear() } }
    val state by viewModel.state.collectAsState()
    val body = (state.body ?: emptySpannedText()).toAnnotatedString()
    SelectionContainer {
        if (body.isEmpty()) {
            Text("Empty body", modifier = modifier.fillMaxSize().padding(16.dp))
        } else {
            Text(
                text = body,
                modifier = modifier.fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                fontFamily = FontFamily.Monospace,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}
