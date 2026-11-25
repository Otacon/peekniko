package peekniko.internal.http

import peekniko.NetworkHeader

private val rasterImageMediaTypes = setOf(
    "image/png",
    "image/x-png",
    "image/apng",
    "image/jpeg",
    "image/jpg",
    "image/pjpeg",
    "image/gif",
    "image/webp",
    "image/bmp",
    "image/x-bmp",
    "image/x-ms-bmp",
    "image/x-icon",
    "image/vnd.microsoft.icon",
    "image/vnd.wap.wbmp",
)

internal fun String.isRasterImageContentType(): Boolean =
    substringBefore(';').trim().lowercase() in rasterImageMediaTypes

internal fun List<NetworkHeader>.contentType(): String? =
    firstOrNull { it.name.equals("content-type", ignoreCase = true) }?.value
