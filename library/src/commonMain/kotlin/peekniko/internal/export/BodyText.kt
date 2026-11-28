package peekniko.internal.export

import peekniko.internal.http.isRasterImageContentType

internal fun decodeTextBody(body: ByteArray, contentType: String?): String? {
    if (contentType?.isRasterImageContentType() == true) return null
    return body.decodeToString()
}
