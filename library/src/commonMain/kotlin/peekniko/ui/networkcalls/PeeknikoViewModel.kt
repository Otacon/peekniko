package peekniko.ui.networkcalls

import peekniko.NetworkCall
import peekniko.NetworkMethod
import peekniko.NetworkRepository
import peekniko.NetworkResponse
import peekniko.pathAndQueryParams
import peekniko.internal.formatting.formatPayloadSize
import peekniko.internal.formatting.toReadableTime
import peekniko.internal.support.ViewModel
import peekniko.ui.PeeknikoCallRowItem
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

public typealias NetworkCallListItem = PeeknikoCallRowItem

internal class PeeknikoViewModel(
    private val networkRepository: NetworkRepository,
    backgroundDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : ViewModel(backgroundDispatcher) {

    private val methodFilter = MutableStateFlow<NetworkMethod?>(null)
    private val statusFilter = MutableStateFlow<CallStatusFilter?>(null)
    private val urlFilter = MutableStateFlow("")
    private val selectedCallId = MutableStateFlow<String?>(null)

    val state: StateFlow<State> = combine(
        networkRepository.networkCalls,
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
            method = method,
            status = status,
            totalCount = calls.size,
            filteredCount = filtered.size,
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

    fun onCallClick(item: PeeknikoCallRowItem) = viewModelScope.launch {
        selectedCallId.value = item.id
    }

    internal fun NetworkCall.toListItem(isSelected: Boolean): PeeknikoCallRowItem {
        val responseTimestamp = response?.timestamp
        val responseCode = (response as? NetworkResponse.Response)?.code
        val exceptionMessage = (response as? NetworkResponse.Error)?.exceptionMessage
        val responseBody = (response as? NetworkResponse.Response)?.body
        val durationMillis = responseTimestamp
            ?.let { (it - request.timestamp).inWholeMilliseconds.coerceAtLeast(0L) }
        val details = buildList {
            durationMillis?.let { add("$it ms") }
            val totalPayloadSize = (request.body?.size?.toLong() ?: 0L) + (responseBody?.size?.toLong() ?: 0L)
            add(totalPayloadSize.formatPayloadSize())
        }.joinToString(" · ")

        val statusBadge = when (response) {
            is NetworkResponse.Error -> "ERR"
            is NetworkResponse.Response -> response.code.toString()
            null -> "..."
        }

        val formattedTime = request.timestamp.toReadableTime(includeMillis = true)
        val subtitle = exceptionMessage ?: "$formattedTime · $details"

        return PeeknikoCallRowItem(
            id = id,
            method = request.method.name,
            urlPath = request.pathAndQueryParams,
            requestTime = request.timestamp.toReadableTime(includeMillis = true),
            details = details,
            responseCode = responseCode,
            exceptionMessage = exceptionMessage,
            statusBadge = statusBadge,
            subtitle = subtitle,
            isSelected = isSelected,
        )
    }

    private fun List<NetworkCall>.filterByUrl(url: String): List<NetworkCall> {
        val trimmedUrl = url.trim()
        if (trimmedUrl.isBlank()) return this

        return this.filter { it.request.pathAndQueryParams.contains(trimmedUrl, ignoreCase = true) }
    }

    private fun List<NetworkCall>.filterByMethod(method: NetworkMethod?): List<NetworkCall> {
        if (method == null) return this
        return this.filter { it.request.method == method }
    }

    private fun List<NetworkCall>.filterByStatus(status: CallStatusFilter?): List<NetworkCall> {
        if (status == null) return this
        return this.filter { call ->
            val responseCode = (call.response as? NetworkResponse.Response)?.code

            when (status) {
                CallStatusFilter.SUCCESS_2XX -> responseCode != null && responseCode in 200..299
                CallStatusFilter.NON_SUCCESS -> responseCode != null && responseCode !in 200..299
                CallStatusFilter.EXCEPTION -> call.response is NetworkResponse.Error
            }
        }
    }

    private fun areFiltersActive(url: String, method: NetworkMethod?, status: CallStatusFilter?): Boolean =
        url.isNotBlank() || method != null || status != null
}

public enum class CallStatusFilter {
    SUCCESS_2XX,
    NON_SUCCESS,
    EXCEPTION;
}

public data class State(
    public val isLoading: Boolean = true,
    public val calls: List<PeeknikoCallRowItem> = emptyList(),
    public val totalCount: Int = 0,
    public val filteredCount: Int = 0,
    public val query: String = "",
    public val method: NetworkMethod? = null,
    public val status: CallStatusFilter? = null,
    public val showResetFilters: Boolean = false,
    public val showClearCalls: Boolean = false,
    public val hasActiveFilters: Boolean = false,
    public val showFilteredEmptyState: Boolean = false,
)
