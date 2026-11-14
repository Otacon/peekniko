package peekniko.ui

import peekniko.NetworkHeader
import peekniko.ui.details.BodyContentType

public data class PeeknikoCallRowItem(
    public val id: String,
    public val method: String,
    public val urlPath: String,
    public val requestTime: String?,
    public val details: String,
    public val responseCode: Int?,
    public val exceptionMessage: String?,
    public val statusBadge: String,
    public val subtitle: String,
    public val isSelected: Boolean,
)

public data class PeeknikoCallDetailItem(
    public val id: String,
    public val title: String,
    public val method: String,
    public val fullUrl: String,
    public val statusCode: Int?,
    public val statusText: String,
    public val statusBadge: String,
    public val exceptionMessage: String?,
    public val requestTime: String,
    public val responseTime: String,
    public val durationText: String,
    public val requestSizeBytes: Long,
    public val responseSizeBytes: Long,
    public val requestSizeText: String,
    public val responseSizeText: String,
    public val totalSizeText: String,
    public val requestBadge: String,
    public val responseBadge: String,
    public val requestHeaders: List<NetworkHeader>,
    public val responseHeaders: List<NetworkHeader>,
    public val overviewHeaders: List<NetworkHeader>,
    public val requestContentType: BodyContentType,
    public val responseContentType: BodyContentType,
    public val rawRequestBody: String?,
    public val rawResponseBody: String?,
    public val curl: String,
)

