package peekniko.ui.details

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import peekniko.NetworkRepository
import peekniko.NetworkResponse
import peekniko.internal.bodyrenderer.*
import peekniko.internal.http.isRasterImageContentType
import peekniko.internal.support.ViewModel

@OptIn(ExperimentalCoroutinesApi::class)
internal class ResponseViewModel(
    repository: NetworkRepository,
    private val themeProvider: BodyRendererThemeProvider,
    private val bodyRenderer: BodyRenderer,
    transactionId: String,
    backgroundDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : ViewModel(backgroundDispatcher) {

    private val theme = MutableStateFlow(themeProvider.theme)
    private val isPrettyPrintEnabled = MutableStateFlow(true)
    private val transaction = repository.observeById(transactionId)

    val state: StateFlow<State> = combine(
        transaction,
        theme,
        isPrettyPrintEnabled
    ) { call, theme, prettyPrintEnabled ->
        Triple(call?.response, theme, prettyPrintEnabled)
    }.transformLatest { (response, theme, prettyPrintEnabled) ->
        emit(State(isLoading = true))
        emit(response.toState(theme = theme, prettyPrintEnabled = prettyPrintEnabled))
    }.flowOn(backgroundDispatcher)
        .stateIn(scope = viewModelScope, started = SharingStarted.Eagerly, initialValue = State())

    fun onViewCreated() {
        theme.value = themeProvider.theme
    }

    fun onPrettyPrintClick(enabled: Boolean) {
        isPrettyPrintEnabled.value = enabled
    }

    private fun NetworkResponse?.toState(theme: BodyRendererTheme, prettyPrintEnabled: Boolean): State {
        return when (this@toState) {
            is NetworkResponse.Error -> {
                val payload = "${exceptionMessage}\n$stackTrace"
                State(
                    isLoading = false,
                    body = Body.Text(spannedText { append(payload) }),
                    prettyPrintState = PrettyPrintState.Unavailable,
                )
            }

            is NetworkResponse.Response -> {
                val contentType = headers.firstOrNull { it.name.equals("content-type", ignoreCase = true) }?.value
                val (renderedBody, prettyPrintState) = if (body != null && body.isNotEmpty() && contentType?.isRasterImageContentType() == true) {
                    Body.Image(body) to PrettyPrintState.Unavailable
                } else {
                    val bodyText = body?.decodeToString().orEmpty()
                    val supportedContentType = contentType?.takeIf { bodyText.isNotBlank() && bodyRenderer.supports(it) }
                    when {
                        supportedContentType == null -> Body.Text(spannedText { append(bodyText) }) to PrettyPrintState.Unavailable

                        prettyPrintEnabled -> {
                            val formatted = bodyRenderer.format(bodyText, supportedContentType) ?: bodyText
                            val highlighted = bodyRenderer.syntaxHighlight(theme, formatted, supportedContentType)
                                ?: spannedText { append(formatted) }
                            Body.Text(highlighted) to PrettyPrintState.Enabled
                        }

                        else -> Body.Text(spannedText { append(bodyText) }) to PrettyPrintState.Disabled
                    }
                }
                State(
                    isLoading = false,
                    body = renderedBody,
                    prettyPrintState = prettyPrintState,
                )
            }

            null -> State(isLoading = false, body = null, prettyPrintState = PrettyPrintState.Unavailable)
        }
    }

    internal data class State(
        val isLoading: Boolean = true,
        val body: Body? = null,
        val prettyPrintState: PrettyPrintState = PrettyPrintState.Unavailable,
    )

    internal sealed interface Body {
        data class Text(val text: SpannedText) : Body

        // Don't make this a data class: data classes compare array properties by reference, not content.
        class Image(val bytes: ByteArray) : Body
    }
}
