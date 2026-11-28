package peekniko.ui.details

import peekniko.NetworkRepository
import peekniko.pathAndQueryParams
import peekniko.internal.support.ViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted

internal class NetworkCallDetailViewModel(
    networkRepository: NetworkRepository,
    transactionId: String?,
    backgroundDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : ViewModel() {

    private val _state = when (transactionId) {
        null -> flowOf(
            State(
                isLoading = false,
                callFound = false,
                title = DEFAULT_TITLE,
                emptyMessage = "Select a transaction to view details"
            )
        )

        else -> {
            networkRepository
                .observeById(transactionId)
                .map { call ->
                    if (call != null) {
                        State(
                            isLoading = false,
                            callFound = true,
                            title = "${call.request.method} ${call.request.pathAndQueryParams}",
                            emptyMessage = ""
                        )
                    } else {
                        State(
                            isLoading = false,
                            callFound = false,
                            title = DEFAULT_TITLE,
                            emptyMessage = "Transaction not found"
                        )
                    }
                }
        }
    }

    val state: StateFlow<State> = _state.flowOn(backgroundDispatcher).stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = State(isLoading = transactionId != null),
    )

    data class State(
        val isLoading: Boolean = true,
        val callFound: Boolean = false,
        val title: String = "",
        val emptyMessage: String = "",
    )

    private companion object {
        const val DEFAULT_TITLE = "Call details"
    }
}
