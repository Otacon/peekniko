package peekniko.ui.details

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import peekniko.NetworkCall
import peekniko.NetworkRepository
import peekniko.NetworkResponse
import peekniko.internal.bodyrenderer.*
import peekniko.internal.http.contentType
import peekniko.internal.http.isRasterImageContentType
import peekniko.internal.support.ViewModel

@OptIn(ExperimentalCoroutinesApi::class)
internal class BodyViewModel(
    repository: NetworkRepository,
    private val themeProvider: BodyRendererThemeProvider,
    private val bodyRenderer: BodyRenderer,
    transactionId: String,
    private val isRequest: Boolean,
    backgroundDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : ViewModel() {

    private val theme = MutableStateFlow(themeProvider.theme)
    private val isPrettyPrintEnabled = MutableStateFlow(true)
    private val transaction = repository.observeById(transactionId)

    val state: StateFlow<State> = combine(
        transaction,
        theme,
        isPrettyPrintEnabled
    ) { call, theme, prettyPrintEnabled ->
        Triple(call, theme, prettyPrintEnabled)
    }.transformLatest { (call, theme, prettyPrintEnabled) ->
        emit(State(isLoading = true))
        emit(call.toState(theme = theme, prettyPrintEnabled = prettyPrintEnabled))
    }.flowOn(backgroundDispatcher)
        .stateIn(scope = viewModelScope, started = SharingStarted.Eagerly, initialValue = State())

    fun onViewCreated() {
        theme.value = themeProvider.theme
    }

    fun onPrettyPrintClick(enabled: Boolean) {
        isPrettyPrintEnabled.value = enabled
    }

    private fun NetworkCall?.toState(theme: BodyRendererTheme, prettyPrintEnabled: Boolean): State {
        val call = this ?: return State(isLoading = false)
        return if (isRequest) {
            renderBody(
                contentType = call.request.headers.contentType(),
                body = call.request.body,
                theme = theme,
                prettyPrintEnabled = prettyPrintEnabled
            )
        } else {
            when (val response = call.response) {
                is NetworkResponse.Response -> renderBody(
                    contentType = response.headers.contentType(),
                    body = response.body,
                    theme = theme,
                    prettyPrintEnabled = prettyPrintEnabled
                )

                is NetworkResponse.Error -> errorState(response)
                null -> State(isLoading = false)
            }
        }
    }

    private fun renderBody(
        contentType: String?,
        body: ByteArray?,
        theme: BodyRendererTheme,
        prettyPrintEnabled: Boolean,
    ): State {
        if (body != null && body.isNotEmpty() && contentType?.isRasterImageContentType() == true) {
            return State(isLoading = false, body = Body.Image(body), prettyPrintState = PrettyPrintState.Unavailable)
        }
        val bodyText = body?.decodeToString().orEmpty()
        val supportedContentType = contentType?.takeIf { bodyText.isNotBlank() && bodyRenderer.supports(it) }
        return when {
            supportedContentType == null -> State(
                isLoading = false,
                body = Body.Text(spannedText { append(bodyText) }),
                prettyPrintState = PrettyPrintState.Unavailable,
            )

            prettyPrintEnabled -> {
                val formatted = bodyRenderer.format(bodyText, supportedContentType) ?: bodyText
                val highlighted = bodyRenderer.syntaxHighlight(theme, formatted, supportedContentType)
                    ?: spannedText { append(formatted) }
                State(
                    isLoading = false,
                    body = Body.Text(highlighted),
                    prettyPrintState = PrettyPrintState.Enabled,
                )
            }

            else -> State(
                isLoading = false,
                body = Body.Text(spannedText { append(bodyText) }),
                prettyPrintState = PrettyPrintState.Disabled,
            )
        }
    }

    private fun errorState(error: NetworkResponse.Error): State = State(
        isLoading = false,
        body = Body.Text(spannedText { append("${error.exceptionMessage}\n${error.stackTrace}") }),
        prettyPrintState = PrettyPrintState.Unavailable,
    )

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

internal enum class PrettyPrintState {
    Enabled, Disabled, Unavailable,
}
