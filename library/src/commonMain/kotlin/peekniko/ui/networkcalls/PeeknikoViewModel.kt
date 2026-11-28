package peekniko.ui.networkcalls

import peekniko.NetworkCallSummary
import peekniko.NetworkMethod
import peekniko.NetworkRepository
import peekniko.pathAndQueryParams
import peekniko.internal.formatting.formatPayloadSize
import peekniko.internal.formatting.toReadableTime
import peekniko.internal.support.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

internal class PeeknikoViewModel(
    private val networkRepository: NetworkRepository,
    backgroundDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : ViewModel() {

    private val methodFilter = MutableStateFlow<NetworkMethod?>(null)
    private val statusFilter = MutableStateFlow<CallStatusFilter?>(null)
    private val urlFilter = MutableStateFlow("")
    private val selectedCallId = MutableStateFlow<String?>(null)

    val state: StateFlow<State> = combine(
        networkRepository.observeCallSummaries(),
        urlFilter,
        methodFilter,
        statusFilter,
        selectedCallId,
    ) { calls, url, method, status, selectedCallId ->
        val filtered = calls
            .filterByUrl(url)
            .filterByMethod(method)
            .filterByStatus(status)
            .map { it.toListItem(selectedCallId == it.id) }
        val hasActiveFilters = areFiltersActive(url, method, status)
        State(
            isLoading = false,
            calls = filtered,
            query = url,
            hasActiveFilters = hasActiveFilters,
            showFilteredEmptyState = hasActiveFilters && calls.isNotEmpty() && filtered.isEmpty(),
            showResetFilters = hasActiveFilters,
            showClearCalls = calls.isNotEmpty(),
        )
    }.flowOn(backgroundDispatcher)
        .stateIn(scope = viewModelScope, started = SharingStarted.Eagerly, initialValue = State())

    fun onSearchQueryChanged(query: String) = urlFilter.update { query }

    fun onMethodFilterSelected(method: NetworkMethod?) = methodFilter.update { method }

    fun onStatusFilterSelected(status: CallStatusFilter?) = statusFilter.update { status }

    fun onClearFiltersClick() {
        statusFilter.value = null
        methodFilter.value = null
        urlFilter.value = ""
    }

    fun onClearClick() = viewModelScope.launch {
        networkRepository.clear()
    }

    fun onCallClick(item: NetworkCallListItem) = viewModelScope.launch {
        selectedCallId.value = item.id
    }

    internal fun NetworkCallSummary.toListItem(isSelected: Boolean): NetworkCallListItem {
        val durationMillis = responseTimestamp?.let { (it - requestTimestamp).inWholeMilliseconds.coerceAtLeast(0L) }
        val details = buildList {
            durationMillis?.let { add("$it ms") }
            add(((requestBodySize ?: 0L) + (responseBodySize ?: 0L)).formatPayloadSize())
        }.joinToString(" · ")

        val statusBadge = when {
            exceptionMessage != null -> "ERR"
            responseCode != null -> responseCode.toString()
            else -> "..."
        }

        val formattedTime = requestTimestamp.toReadableTime(includeMillis = true)
        val subtitle = exceptionMessage ?: "$formattedTime · $details"

        return NetworkCallListItem(
            id = id,
            method = method.name,
            urlPath = pathAndQueryParams,
            requestTime = formattedTime,
            details = details,
            responseCode = responseCode,
            exceptionMessage = exceptionMessage,
            statusBadge = statusBadge,
            subtitle = subtitle,
            isSelected = isSelected,
        )
    }

    private fun List<NetworkCallSummary>.filterByUrl(url: String): List<NetworkCallSummary> {
        val trimmedUrl = url.trim()
        if (trimmedUrl.isBlank()) return this

        return this.filter { it.pathAndQueryParams.contains(trimmedUrl, ignoreCase = true) }
    }

    private fun List<NetworkCallSummary>.filterByMethod(method: NetworkMethod?): List<NetworkCallSummary> {
        if (method == null) return this
        return this.filter { it.method == method }
    }

    private fun List<NetworkCallSummary>.filterByStatus(status: CallStatusFilter?): List<NetworkCallSummary> {
        if (status == null) return this
        return this.filter { call ->
            val responseCode = call.responseCode

            when (status) {
                CallStatusFilter.SUCCESS_2XX -> responseCode != null && responseCode in 200..299
                CallStatusFilter.NON_SUCCESS -> responseCode != null && responseCode !in 200..299
                CallStatusFilter.EXCEPTION -> call.exceptionMessage != null
            }
        }
    }

    private fun areFiltersActive(url: String, method: NetworkMethod?, status: CallStatusFilter?): Boolean =
        url.isNotBlank() || method != null || status != null
}

internal enum class CallStatusFilter {
    SUCCESS_2XX,
    NON_SUCCESS,
    EXCEPTION;
}

internal data class State(
    val isLoading: Boolean = true,
    val calls: List<NetworkCallListItem> = emptyList(),
    val query: String = "",
    val showResetFilters: Boolean = false,
    val showClearCalls: Boolean = false,
    val hasActiveFilters: Boolean = false,
    val showFilteredEmptyState: Boolean = false,
)

internal data class NetworkCallListItem(
    val id: String,
    val method: String,
    val urlPath: String,
    val requestTime: String?,
    val details: String,
    val responseCode: Int?,
    val exceptionMessage: String?,
    val statusBadge: String,
    val subtitle: String,
    val isSelected: Boolean,
)
