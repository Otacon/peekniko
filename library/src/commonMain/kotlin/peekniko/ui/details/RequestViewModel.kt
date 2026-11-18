package peekniko.ui.details

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import peekniko.NetworkRepository
import peekniko.NetworkRequest
import peekniko.internal.bodyrenderer.*
import peekniko.internal.support.ViewModel

internal class RequestViewModel(
    repository: NetworkRepository,
    private val themeProvider: BodyRendererThemeProvider,
    private val bodyRenderer: BodyRendererComposite,
    private val noopBodyRenderer: BodyRenderer,
    transactionId: String,
) : ViewModel() {

    private val theme = MutableStateFlow(themeProvider.theme)
    private val isPrettyPrintEnabled = MutableStateFlow(true)
    private val transaction = repository.observeById(transactionId)

    val state: StateFlow<State> = combine(
        transaction,
        theme,
        isPrettyPrintEnabled
    ) { call, theme, prettyPrintEnabled ->
        call?.request.toState(theme = theme, prettyPrint = prettyPrintEnabled)
    }.stateIn(scope = viewModelScope, started = SharingStarted.Eagerly, initialValue = State())

    fun onViewCreated() {
        theme.value = themeProvider.theme
    }

    fun onPrettyPrintClick(enabled: Boolean) {
        isPrettyPrintEnabled.value = enabled
    }

    private suspend fun NetworkRequest?.toState(
        theme: BodyRendererTheme,
        prettyPrint: Boolean,
    ) = withContext(Dispatchers.Default) {
        if (this@toState == null) {
            return@withContext State(
                isLoading = false,
                body = emptySpannedText(),
                isPrettyPrintEnabled = prettyPrint,
            )
        }
        val bodyText = body?.decodeToString().orEmpty()
        val rawContentType = headers.firstOrNull { it.name.equals("content-type", ignoreCase = true) }?.value
        val renderedBody = if (prettyPrint) {
            val formatted = bodyRenderer.format(bodyText, rawContentType)
            bodyRenderer.syntaxHighlight(theme, formatted, rawContentType)
        } else {
            noopBodyRenderer.syntaxHighlight(theme, bodyText, null).build()
        }
        val contentType = bodyRenderer.contentType(rawContentType)
        State(
            isLoading = false,
            body = renderedBody,
            contentType = contentType,
            isPrettyPrintEnabled = prettyPrint,
        )
    }

    internal data class State(
        val isLoading: Boolean = true,
        val body: SpannedText? = null,
        val contentType: BodyContentType = BodyContentType.PLAIN_TEXT,
        val isPrettyPrintEnabled: Boolean = true,
    )
}
