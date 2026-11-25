package peekniko.internal.support

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal suspend fun ByteArray.decodeBitmap(): Bitmap? = withContext(Dispatchers.Default) {
    try {
        BitmapFactory.decodeByteArray(this@decodeBitmap, 0, size)
    } catch (_: Exception) {
        null
    }
}
