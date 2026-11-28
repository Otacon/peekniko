package peekniko.ui.details

import peekniko.NetworkCall
import peekniko.NetworkHeader
import peekniko.NetworkRepository
import peekniko.NetworkResponse
import peekniko.fullUrl
import peekniko.pathAndQueryParams
import peekniko.internal.formatting.formatPayloadSize
import peekniko.internal.formatting.toReadableTime
import peekniko.internal.http.httpMessage
import peekniko.internal.support.ViewModel
import peekniko.ui.PeeknikoCallDetailItem
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
) : ViewModel(backgroundDispatcher) {

    private val _state = when (transactionId) {
        null -> flowOf(
            State(
                transaction = null,
                isLoading = false,
                title = "Call details",
                emptyMessage = "Select a transaction to view details"
            )
        )

        else -> {
            networkRepository
                .observeById(transactionId)
                .map {
                    val transactionDetail = it?.toDetailItem()
                    if (transactionDetail != null) {
                        State(
                            transaction = transactionDetail,
                            isLoading = false,
                            title = transactionDetail.title,
                            emptyMessage = ""
                        )
                    } else {
                        State(
                            transaction = null,
                            isLoading = false,
                            title = "Call details",
                            emptyMessage = "Transaction not found"
                        )
                    }
                }
        }
    }

    internal val state: StateFlow<State> = _state.flowOn(backgroundDispatcher).stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = State(isLoading = transactionId != null),
    )

    data class State(
        val transaction: PeeknikoCallDetailItem? = null,
        val isLoading: Boolean = true,
        val title: String = "",
        val emptyMessage: String = "",
    )

    private fun NetworkCall.toDetailItem(): PeeknikoCallDetailItem = when (val result = response) {
        is NetworkResponse.Error -> toErrorDetailItem(result)
        is NetworkResponse.Response -> toResponseDetailItem(result)
        null -> toPendingDetailItem()
    }

    private fun NetworkCall.toErrorDetailItem(error: NetworkResponse.Error): PeeknikoCallDetailItem =
        buildDetailItem(
            statusCode = null,
            statusText = error.exceptionMessage,
            statusBadge = "ERROR",
            exceptionMessage = error.exceptionMessage,
            responseBody = null,
            responseHeaders = emptyList(),
            responseTimestamp = error.timestamp,
        )

    private fun NetworkCall.toResponseDetailItem(result: NetworkResponse.Response): PeeknikoCallDetailItem {
        val message = result.code.httpMessage()
        val statusText = result.code.toString() + message?.let { " - $it" }.orEmpty()
        return buildDetailItem(
            statusCode = result.code,
            statusText = statusText,
            statusBadge = result.code.toString(),
            exceptionMessage = null,
            responseBody = result.body,
            responseHeaders = result.headers,
            responseTimestamp = result.timestamp,
        )
    }

    private fun NetworkCall.toPendingDetailItem(): PeeknikoCallDetailItem = buildDetailItem(
        statusCode = null,
        statusText = "Running...",
        statusBadge = "Pending",
        exceptionMessage = null,
        responseBody = null,
        responseHeaders = emptyList(),
        responseTimestamp = null,
    )

    private fun NetworkCall.buildDetailItem(
        statusCode: Int?,
        statusText: String,
        statusBadge: String,
        exceptionMessage: String?,
        responseBody: ByteArray?,
        responseHeaders: List<NetworkHeader>,
        responseTimestamp: kotlin.time.Instant?,
    ): PeeknikoCallDetailItem {
        val requestBytes = request.body?.size?.toLong() ?: 0L
        val responseBytes = responseBody?.size?.toLong() ?: 0L
        val requestSizeText = requestBytes.formatPayloadSize()
        val responseSizeText = responseBytes.formatPayloadSize()
        val totalSizeText = (requestBytes + responseBytes).formatPayloadSize()
        val durationText = responseTimestamp
            ?.let { ((it - request.timestamp).inWholeMilliseconds.coerceAtLeast(0L)).let { ms -> "$ms ms" } }
            ?: "Pending"
        val requestTime = request.timestamp.toReadableTime()
        val responseTime = responseTimestamp?.toReadableTime() ?: "Pending"
        val overview = buildList {
            add(NetworkHeader("Full URL", request.fullUrl))
            add(NetworkHeader("Method", request.method.name))
            add(NetworkHeader("Status", statusText))
            add(NetworkHeader("Request Time", requestTime))
            add(NetworkHeader("Response Time", responseTime))
            add(NetworkHeader("Duration", durationText))
            add(NetworkHeader("Request Size", requestSizeText))
            add(NetworkHeader("Response Size", responseSizeText))
            add(NetworkHeader("Total Size", totalSizeText))
            if (!exceptionMessage.isNullOrEmpty()) add(NetworkHeader("Error", exceptionMessage))
        }

        return PeeknikoCallDetailItem(
            id = id,
            title = "${request.method} ${request.pathAndQueryParams}",
            method = request.method.name,
            fullUrl = request.fullUrl,
            statusCode = statusCode,
            statusText = statusText,
            statusBadge = statusBadge,
            exceptionMessage = exceptionMessage,
            requestTime = requestTime,
            responseTime = responseTime,
            durationText = durationText,
            requestSizeBytes = requestBytes,
            responseSizeBytes = responseBytes,
            requestSizeText = requestSizeText,
            responseSizeText = responseSizeText,
            totalSizeText = totalSizeText,
            requestBadge = if (requestBytes > 0) "($requestSizeText)" else "(Empty)",
            responseBadge = if (responseBytes > 0) "($responseSizeText)" else "(Empty)",
            requestHeaders = request.headers,
            responseHeaders = responseHeaders,
            overviewHeaders = overview,
            rawRequestBody = request.body?.decodeToString(),
            rawResponseBody = responseBody?.decodeToString(),
        )
    }

}
