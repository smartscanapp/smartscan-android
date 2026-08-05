package com.fpf.smartscan.ui.components.media

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.zIndex
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.request.crossfade
import com.fpf.smartscan.core.media.MediaType

@Composable
fun ImageDisplay(
    uri: Uri,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    mediaType: MediaType? = null,
    maxSize:Int? = 512,
    onSizeChanged: ((Int, Int) -> Unit)? = null,
    onError: ((error:  AsyncImagePainter.State.Error)-> Unit)? = null
    ) {
    val context = LocalContext.current

    val request = remember(uri, maxSize) {
        val builder = ImageRequest.Builder(context)
            .allowHardware(true)
            .crossfade(true)
            .data(uri)

        if (maxSize != null) {
            builder.size(maxSize)
        }

        builder.build()
    }

    Box(modifier = Modifier.background(Color.Transparent), contentAlignment = Alignment.Center) {
        if(mediaType == MediaType.VIDEO){
            Icon(Icons.Filled.PlayCircle, contentDescription = null, modifier = Modifier
                .align(
                    Alignment.Center
                )
                .zIndex(1f))
        }
        AsyncImage(
            model = request,
            contentDescription = "Displayed image",
            contentScale = contentScale,
            modifier = modifier.fillMaxWidth(),
            onError = { onError?.invoke(it) },
            onSuccess = {
                onSizeChanged?.invoke(
                    it.result.image.width,
                    it.result.image.height
                )
            }
        )
    }
}
