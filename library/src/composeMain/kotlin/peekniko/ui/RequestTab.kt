package peekniko.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import peekniko.Peekniko
import peekniko.internal.bodyrenderer.emptySpannedText
import peekniko.internal.bodyrenderer.toAnnotatedString
import peekniko.ui.details.RequestViewModel

@Composable
internal fun RequestTab(callId: String, modifier: Modifier = Modifier) {
    val viewModel = retainedViewModel(RequestViewModel::class) { Peekniko.createRequestViewModel(callId) }
    val state by viewModel.state.collectAsState()
    TopBarActionsEffect {
        PrettyPrintAction(
            prettyPrintState = state.prettyPrintState,
            onPrettyPrintClick = viewModel::onPrettyPrintClick,
        )
    }
    BodyText((state.body ?: emptySpannedText()).toAnnotatedString(), modifier)
}

@Composable
private fun BodyText(text: AnnotatedString, modifier: Modifier) {
    SelectionContainer {
        if (text.isEmpty()) {
            Text(
                modifier = modifier
                    .fillMaxSize()
                    .padding(16.dp),
                text = "Empty body"
            )
        } else {
            Text(
                text = text,
                modifier = modifier.fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                fontFamily = FontFamily.Monospace,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}
