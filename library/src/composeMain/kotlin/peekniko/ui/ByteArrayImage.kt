package peekniko.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.skia.Image

@Composable
internal fun ByteArrayImage(
    bytes: ByteArray,
    contentDescription: String?,
    modifier: Modifier = Modifier,
) {
    val decoded by produceState<DecodedImage>(DecodedImage.Loading, bytes) {
        value = withContext(Dispatchers.Default) {
            runCatching {
                val bitmap = Image.makeFromEncoded(bytes).toComposeImageBitmap()
                DecodedImage.Ready(bitmap)
            }.getOrDefault(DecodedImage.Failed)
        }
    }
    when (val image = decoded) {
        DecodedImage.Loading -> Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }

        DecodedImage.Failed -> Text(
            modifier = modifier
                .fillMaxSize()
                .padding(16.dp),
            text = "Unable to render image"
        )

        is DecodedImage.Ready -> Image(
            modifier = modifier
                .fillMaxSize()
                .padding(16.dp),
            bitmap = image.bitmap,
            contentDescription = contentDescription,
            contentScale = ContentScale.Fit,
            alignment = Alignment.TopCenter,
        )
    }
}

private sealed interface DecodedImage {
    data object Loading : DecodedImage
    data object Failed : DecodedImage
    class Ready(val bitmap: ImageBitmap) : DecodedImage
}
