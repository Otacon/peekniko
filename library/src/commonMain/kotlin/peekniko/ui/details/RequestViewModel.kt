package peekniko.ui.details

import peekniko.NetworkRepository
import peekniko.NetworkRequest
import peekniko.internal.bodyrenderer.BodyRendererComposite
import peekniko.internal.bodyrenderer.BodyRendererTheme
import peekniko.internal.bodyrenderer.BodyRendererThemeProvider
import peekniko.internal.bodyrenderer.SpannedText
import peekniko.internal.bodyrenderer.emptySpannedText
import peekniko.internal.support.ViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext

@OptIn(ExperimentalCoroutinesApi::class)
internal class RequestViewModel(
    repository: NetworkRepository,
    private val themeProvider: BodyRendererThemeProvider,
    private val bodyRenderer: BodyRendererComposite,
    transactionId: String,
) : ViewModel() {

    private val theme = MutableStateFlow(themeProvider.theme)
    private val transaction = repository.observeById(transactionId)

    val state: StateFlow<State> = combine(transaction, theme) { call, theme -> call?.request to theme }
        .mapLatest { (request, currentTheme) -> request.toState(currentTheme) }
        .stateIn(scope = viewModelScope, started = SharingStarted.Eagerly, initialValue = State())

    fun onViewCreated() {
        theme.value = themeProvider.theme
    }

    private suspend fun NetworkRequest?.toState(theme: BodyRendererTheme) = withContext(Dispatchers.Default) {
        if (this@toState == null) return@withContext State(isLoading = false, body = emptySpannedText())
        val bodyText = body?.decodeToString().orEmpty()
        val rawContentType = headers.firstOrNull { it.name.equals("content-type", ignoreCase = true) }?.value
        val formatted = bodyRenderer.format(bodyText, rawContentType)
        val highlighted = bodyRenderer.syntaxHighlight(theme, formatted, rawContentType)
        val contentType = bodyRenderer.contentType(rawContentType)
        State(
            isLoading = false,
            body = highlighted,
            contentType = contentType,
        )
    }

    internal data class State(
        val isLoading: Boolean = true,
        val body: SpannedText? = null,
        val contentType: BodyContentType = BodyContentType.PLAIN_TEXT,
    )
}
