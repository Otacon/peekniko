package peekniko.ui.details

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import peekniko.NetworkRepository
import peekniko.NetworkResponse
import peekniko.internal.bodyrenderer.*
import peekniko.internal.support.ViewModel

internal class ResponseViewModel(
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
    ) { call, theme, prettyPrint ->
        call?.response.toState(theme, prettyPrint)
    }.stateIn(scope = viewModelScope, started = SharingStarted.Eagerly, initialValue = State())

    fun onViewCreated() {
        theme.value = themeProvider.theme
    }

    fun onPrettyPrintClick(enabled: Boolean) {
        isPrettyPrintEnabled.value = enabled
    }

    private suspend fun NetworkResponse?.toState(
        theme: BodyRendererTheme,
        prettyPrint: Boolean,
    ) = withContext(Dispatchers.Default) {
        when (this@toState) {
            is NetworkResponse.Error -> {
                val payload = "${exceptionMessage}\n$stackTrace"
                val errorText = noopBodyRenderer.syntaxHighlight(
                    theme = theme,
                    payload = payload,
                    contentType = null,
                ).build()
                State(
                    isLoading = false,
                    body = errorText,
                    contentType = BodyContentType.PLAIN_TEXT,
                    isPrettyPrintEnabled = prettyPrint,
                )
            }

            is NetworkResponse.Response -> {
                val body = body?.decodeToString().orEmpty()
                val rawContentType = headers
                    .firstOrNull { it.name.equals("content-type", ignoreCase = true) }
                    ?.value

                val renderedBody = if (prettyPrint) {
                    val formatted = bodyRenderer.format(body, rawContentType)
                    bodyRenderer.syntaxHighlight(theme, formatted, rawContentType)
                } else {
                    noopBodyRenderer.syntaxHighlight(theme = theme, payload = body, contentType = null).build()
                }
                val contentType = bodyRenderer.contentType(rawContentType)
                State(
                    isLoading = false,
                    body = renderedBody,
                    contentType = contentType,
                    isPrettyPrintEnabled = prettyPrint,
                )
            }

            null -> State(isLoading = false, isPrettyPrintEnabled = prettyPrint)
        }
    }

    internal data class State(
        val isLoading: Boolean = true,
        val body: SpannedText? = null,
        val contentType: BodyContentType = BodyContentType.PLAIN_TEXT,
        val isPrettyPrintEnabled: Boolean = true,
    )
}
