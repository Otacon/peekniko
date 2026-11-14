package peekniko.ui.details

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext
import peekniko.NetworkCall
import peekniko.NetworkHeader
import peekniko.NetworkRepository
import peekniko.NetworkResponse
import peekniko.fullUrl
import peekniko.internal.bodyrenderer.SpannedText
import peekniko.internal.bodyrenderer.SpannedTextBuilder
import peekniko.internal.bodyrenderer.emptySpannedText
import peekniko.internal.bodyrenderer.spannedText
import peekniko.internal.http.httpMessage
import peekniko.internal.support.ViewModel

internal class SummaryViewModel(
    repository: NetworkRepository,
    transactionId: String,
) : ViewModel() {
    private val transaction = repository.observeById(transactionId)

    val state: StateFlow<State> = transaction
        .map { it.toHeadersUiState() }
        .stateIn(scope = viewModelScope, started = SharingStarted.Eagerly, initialValue = State())

    private suspend fun NetworkCall?.toHeadersUiState(): State = withContext(Dispatchers.Default) {
        val call = this@toHeadersUiState ?: return@withContext State(isLoading = false)
        when (val result = call.response) {
            is NetworkResponse.Error -> call.toErrorHeadersUiState(result)
            is NetworkResponse.Response -> call.toResponseHeadersUiState(result)
            null -> call.toPendingHeadersUiState()
        }
    }

    private fun NetworkCall.toErrorHeadersUiState(error: NetworkResponse.Error): State =
        toHeadersUiState(
            status = error.exceptionMessage,
            responseHeaders = emptyList(),
        )

    private fun NetworkCall.toResponseHeadersUiState(response: NetworkResponse.Response): State {
        val status = response.code.httpMessage()?.let { "${response.code} - $it" } ?: response.code.toString()
        return toHeadersUiState(status, response.headers)
    }

    private fun NetworkCall.toPendingHeadersUiState(): State =
        toHeadersUiState(status = "Running...", responseHeaders = emptyList())

    private fun NetworkCall.toHeadersUiState(status: String, responseHeaders: List<NetworkHeader>): State {
        val generalText = buildString {
            appendKeyValue("Full URL", request.fullUrl, "\n")
            appendKeyValue("Method", request.method.name, "\n")
            appendKeyValue("Status code", status)
        }
        val general = spannedText {
            appendKeyValue("Full URL", request.fullUrl, "\n")
            appendKeyValue("Method", request.method.name, "\n")
            appendKeyValue("Status code", status)
        }.build()
        val requestHeadersText = request.headers.toPlainText()
        val responseHeadersText = responseHeaders.toPlainText()
        return State(
            isLoading = false,
            general = general,
            responseHeaders = responseHeaders.toSpannedText(),
            requestHeaders = request.headers.toSpannedText(),
            generalText = generalText,
            requestHeadersText = requestHeadersText,
            responseHeadersText = responseHeadersText,
        )
    }

    private fun List<NetworkHeader>.toPlainText(): String = if (isEmpty()) "(none)" else joinToString("\n") {
        "${it.name}: ${it.value}"
    }

    private fun StringBuilder.appendKeyValue(name: String, value: String, suffix: String = "") {
        append(name).append(": ").append(value).append(suffix)
    }

    private fun List<NetworkHeader>.toSpannedText(): SpannedText = spannedText {
        if (isEmpty()) {
            append("(none)")
        } else {
            forEachIndexed { index, header ->
                val suffix = if (index < lastIndex) "\n" else ""
                appendKeyValue(header.name, header.value, suffix)
            }
        }
    }.build()

    private fun SpannedTextBuilder.appendKeyValue(name: String, value: String, suffix: String = "") {
        append("$name: ", isBold = true)
        append("$value$suffix")
    }

    internal data class State(
        val isLoading: Boolean = true,
        val general: SpannedText = emptySpannedText(),
        val responseHeaders: SpannedText = emptySpannedText(),
        val requestHeaders: SpannedText = emptySpannedText(),
        val generalText: String = "",
        val requestHeadersText: String = "",
        val responseHeadersText: String = "",
    )
}
