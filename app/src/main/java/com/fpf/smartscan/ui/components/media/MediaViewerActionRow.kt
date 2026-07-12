package com.fpf.smartscan.ui.components.media

import android.content.ClipData
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalClipboard
import com.fpf.smartscan.media.MediaType
import com.fpf.smartscan.media.openImageInGallery
import com.fpf.smartscan.media.openVideoInGallery
import com.fpf.smartscan.media.shareMedia
import com.fpf.smartscan.ui.components.common.ActionRowWithFade
import com.fpf.smartscan.utils.canOpenUri


@Composable
fun MediaViewerActionRow(
    uri: Uri,
    type: MediaType,
    onClose: () -> Unit,
    onUpdateSearchImage: ((uri: Uri) -> Unit)?,
    isVisible: Boolean
) {
    val context = LocalContext.current
    val clipboard = LocalClipboard.current
    val isUriAccessible = canOpenUri(context, uri)

    ActionRowWithFade(visible = isVisible) {
        IconButton(onClick = { onClose() }) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Close Image",
                tint = MaterialTheme.colorScheme.onBackground
            )
        }

        if (isUriAccessible) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                IconButton(onClick = { shareMedia(context, uri) }) {
                    Icon(
                        Icons.Filled.Share,
                        contentDescription = "Share",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                if(type == MediaType.IMAGE) {
                    IconButton(onClick = {
                        clipboard.nativeClipboard.setPrimaryClip(
                            ClipData.newUri(context.contentResolver, "smartscan_media", uri)
                        )
                    }) {
                        Icon(
                            Icons.Filled.ContentCopy,
                            contentDescription = "Copy to clipboard",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                IconButton(onClick = {
                    if (type == MediaType.IMAGE) {
                        openImageInGallery(context, uri)
                    } else {
                        openVideoInGallery(context, uri)
                    }
                }) {
                    Icon(
                        Icons.Filled.PhotoLibrary,
                        contentDescription = "Open in Gallery",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                if (type == MediaType.IMAGE && onUpdateSearchImage != null) {
                    IconButton(onClick = { onUpdateSearchImage(uri) }) {
                        Icon(
                            Icons.Filled.Search,
                            contentDescription = "Search image",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

