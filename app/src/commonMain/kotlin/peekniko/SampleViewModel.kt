package peekniko

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import peekniko.playground.PlaygroundHttpClient
import peekniko.playground.connectionFailureRequest
import peekniko.playground.playgroundRequests
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal class SampleViewModel(
    private val mockServer: MockServer? = AppModule.mockServer,
    private val okHttpClient: PlaygroundHttpClient? = AppModule.okhttp,
    private val urlSessionClient: PlaygroundHttpClient? = AppModule.urlSession,
    private val ktorClient: PlaygroundHttpClient? = AppModule.ktor,
) : ViewModel() {
    private val _state = MutableStateFlow(
        SampleUiState(
            showOkHttpButton = okHttpClient != null,
            showUrlSessionButton = urlSessionClient != null,
        )
    )
    val state = _state.asStateFlow()

    private val _events = MutableSharedFlow<SampleEvent>()
    val events = _events.asSharedFlow()

    init {
        viewModelScope.launch {
            if (mockServer == null) {
                _state.update {
                    it.copy(
                        status = "Choose a client to run the scenarios.",
                        isLoading = false,
                        showLocalServerToggle = false,
                        isLocalServerEnabled = false,
                    )
                }
                return@launch
            }
            val result = runCatching { withContext(Dispatchers.Default) { mockServer.start() } }
            _state.value = _state.value.copy(
                status = result.fold(
                    { "Local MockWebServer listening at ${mockServer.baseUrl}. Choose a client to run the scenarios." },
                    { "Local MockWebServer failed to start: ${it.message}. Turn off local server to use httpbin.org." },
                ),
                isLoading = false,
                showLocalServerToggle = true,
                isLocalServerEnabled = result.isSuccess,
            )
        }
    }

    fun onLocalServerToggled(enabled: Boolean) {
        _state.update {
            it.copy(
                isLocalServerEnabled = enabled,
                status = if (enabled) "Using local MockWebServer" else "Using https://httpbin.org/",
            )
        }
    }

    fun onRunKtor() = viewModelScope.launch {
        ktorClient?.let { runPlayground("Ktor", it) }
    }

    fun onRunOkHttp() = viewModelScope.launch {
        okHttpClient?.let { runPlayground("OkHttp", it) }
    }

    fun onRunUrlSession() {
        urlSessionClient?.let { client -> viewModelScope.launch { runPlayground("URLSession", client) } }
    }

    private suspend fun runPlayground(clientName: String, client: PlaygroundHttpClient) {
        val mockBaseUrl = mockServer?.baseUrl
        val useMock = _state.value.isLocalServerEnabled && mockBaseUrl != null
        _state.update { it.copy(isLoading = true, status = "Running $clientName requests...") }
        val result = runCatching {
            withContext(Dispatchers.Default) {
                val baseUrl = if (useMock) mockBaseUrl else "https://httpbin.org"
                playgroundRequests.forEach { client.execute(baseUrl, it) }
                if (useMock) {
                    val failure = runCatching { client.execute(baseUrl, connectionFailureRequest) }.exceptionOrNull()
                    if (failure is CancellationException) throw failure
                    checkNotNull(failure) { "Connection-failure scenario unexpectedly succeeded" }
                    "Requests completed; expected connection failure: ${failure.message ?: failure::class.simpleName}"
                } else {
                    "Requests completed"
                }
            }
        }
        result.exceptionOrNull()?.let { if (it is CancellationException) throw it }
        val message = result.getOrElse { "Requests failed ${it.message}" }
        _state.update { it.copy(isLoading = false, status = message) }
    }

    fun onOpenPeeknikoClick() = viewModelScope.launch {
        _events.emit(SampleEvent.OpenPeekniko)
    }

    override fun onCleared() {
        mockServer?.close()
        okHttpClient?.close()
        ktorClient?.close()
        urlSessionClient?.close()
    }
}

data class SampleUiState(
    val isLoading: Boolean = false,
    val showLocalServerToggle: Boolean = true,
    val isLocalServerEnabled: Boolean = false,
    val showOkHttpButton: Boolean = false,
    val showUrlSessionButton: Boolean = false,
    val status: String = "",
)

internal sealed interface SampleEvent {
    data object OpenPeekniko : SampleEvent

}
