package peekniko.ui.details

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import peekniko.NetworkCall
import peekniko.NetworkRepository
import peekniko.NetworkResponse
import peekniko.fullUrl
import peekniko.internal.bodyrenderer.SpannedText
import peekniko.internal.bodyrenderer.spannedText
import peekniko.internal.formatting.TimeFormatter
import peekniko.internal.http.httpMessage
import peekniko.internal.support.ViewModel
import peekniko.isSsl

internal class SummaryViewModel(
    repository: NetworkRepository,
    transactionId: String,
    backgroundDispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val timeFormatter: TimeFormatter,
) : ViewModel(backgroundDispatcher) {
    private val transaction = repository.observeById(transactionId)

    val state: StateFlow<State> = transaction
        .map { it.toHeadersUiState() }
        .flowOn(backgroundDispatcher)
        .stateIn(scope = viewModelScope, started = SharingStarted.Eagerly, initialValue = State())

    private fun NetworkCall?.toHeadersUiState(): State {
        val call = this ?: return State(isLoading = false)
        return when (val result = call.response) {
            is NetworkResponse.Error -> call.toErrorHeadersUiState(result)
            is NetworkResponse.Response -> call.toResponseHeadersUiState(result)
            null -> call.toPendingHeadersUiState()
        }
    }

    private fun NetworkCall.toErrorHeadersUiState(error: NetworkResponse.Error): State =
        toHeadersUiState(status = error.exceptionMessage, response = null)

    private fun NetworkCall.toResponseHeadersUiState(response: NetworkResponse.Response): State {
        val status = response.code.httpMessage()?.let { "${response.code} - $it" } ?: response.code.toString()
        return toHeadersUiState(status, response)
    }

    private fun NetworkCall.toPendingHeadersUiState(): State =
        toHeadersUiState(status = "Running...", response = null)

    private fun NetworkCall.toHeadersUiState(status: String, response: NetworkResponse.Response?): State {
        val completedAt = this.response?.timestamp
        val durationMs = completedAt?.let { (it - request.timestamp).inWholeMilliseconds.coerceAtLeast(0L) }
        val general = listOf(
            "URL" to request.fullUrl,
            "Method" to request.method.name,
            "Code" to status,
            "Request time" to timeFormatter.format(request.timestamp),
            "Response time" to completedAt?.let(timeFormatter::format),
            "Duration" to durationMs?.let { "$it ms" },
            "Protocol" to response?.protocol,
            "SSL" to if (request.isSsl) "Yes" else "No",
            "TLS version" to response?.tlsVersion,
            "Cipher suite" to response?.cipherSuite,
        ).mapNotNull { (name, value) -> value?.takeIf { it.isNotBlank() }?.let { name to it } }
        return State(
            isLoading = false,
            general = general.toSpannedText(),
            requestHeaders = request.headers.map { it.name to it.value }.toSpannedText(),
            responseHeaders = response?.headers.orEmpty().map { it.name to it.value }.toSpannedText(),
        )
    }

    private fun List<Pair<String, String>>.toSpannedText(): SpannedText = spannedText {
        forEachIndexed { index, (name, value) ->
            append("$name: ", isBold = true)
            append(if (index < lastIndex) "$value\n" else value)
        }
    }

    internal data class State(
        val isLoading: Boolean = true,
        val general: SpannedText = spannedText(),
        val responseHeaders: SpannedText = spannedText(),
        val requestHeaders: SpannedText = spannedText(),
    )
}
