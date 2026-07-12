package com.fpf.smartscan.ui.components.media

import android.content.ClipData
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.fpf.smartscan.media.MediaItem
import com.fpf.smartscan.media.MediaType
import com.fpf.smartscan.media.openImageInGallery
import com.fpf.smartscan.media.openVideoInGallery
import com.fpf.smartscan.media.shareMedia
import com.fpf.smartscan.ui.components.common.ActionRowWithFade
import com.fpf.smartscan.utils.canOpenUri

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaViewer(
    items: List<MediaItem>,
    initialIndex: Int,
    onClose: () -> Unit,
    onLoadMore: (() -> Unit)? = null,
    onUpdateSearchImage: ((uri: Uri) -> Unit)? = null,
    onSaveUpdatedItem: (MediaItem) -> Unit,
    maxSize: Int? = 1024
) {
    if (items.isEmpty()) return

    var isActionsVisible by remember { mutableStateOf(true) }
    var isDescriptionVisible by remember { mutableStateOf(false) }

    var currentIndex by remember {
        mutableIntStateOf(initialIndex.coerceIn(0, items.lastIndex))
    }

    val currentItem = items[currentIndex]

    Popup(
        onDismissRequest = { onClose() },
        properties = PopupProperties(
            dismissOnBackPress = true,
            focusable = true
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {

            if (currentItem.type == MediaType.IMAGE) {
                ImageDisplay(
                    uri = currentItem.uri,
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(currentIndex) {
                            detectHorizontalDragGestures(
                                onHorizontalDrag = { _, dragAmount ->
                                    if (dragAmount < -20 &&
                                        currentIndex < items.lastIndex
                                    ) {
                                        currentIndex++

                                        if (currentIndex == items.lastIndex - 1) {
                                            onLoadMore?.invoke()
                                        }
                                    } else if (
                                        dragAmount > 20 &&
                                        currentIndex > 0
                                    ) {
                                        currentIndex--
                                    }
                                }
                            )
                        },
                    contentScale = ContentScale.FillWidth,
                    maxSize = maxSize,
                    mediaType = currentItem.type
                )
            } else {
                VideoDisplay(
                    uri = currentItem.uri,
                    modifier = Modifier.fillMaxSize(),
                    onTap = {
                        isActionsVisible = !isActionsVisible
                    },
                    onSwipeLeft = {
                        if (currentIndex < items.lastIndex) {
                            currentIndex++

                            if (currentIndex == items.lastIndex - 1) {
                                onLoadMore?.invoke()
                            }
                        }
                    },
                    onSwipeRight = {
                        if (currentIndex > 0) {
                            currentIndex--
                        }
                    }
                )
            }

            ActionRow(
                uri = currentItem.uri,
                type = currentItem.type,
                onClose = onClose,
                onUpdateSearchImage = onUpdateSearchImage,
                isVisible = isActionsVisible
            )

            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 24.dp)
            ) {
                IconButton(
                    onClick = {
                        isDescriptionVisible = true
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowUp,
                        contentDescription = "Show description"
                    )
                }
            }
        }
    }


    if (isDescriptionVisible) {
        ModalBottomSheet(
            onDismissRequest = {
                isDescriptionVisible = false
            }
        ) {
            MediaViewerDescriptionView(
                description = currentItem.description,
                onSave = { updatedDescription ->
                    onSaveUpdatedItem(
                        currentItem.copy(description = updatedDescription),
                    )
                }
            )
        }
    }
}

@Composable
fun ActionRow(
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

