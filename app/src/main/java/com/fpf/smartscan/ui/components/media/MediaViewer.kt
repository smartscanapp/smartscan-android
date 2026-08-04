package com.fpf.smartscan.ui.components.media

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.fpf.smartscan.core.media.CollectionType
import com.fpf.smartscan.core.media.MediaItem
import com.fpf.smartscan.core.media.MediaType
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaViewer(
    items: List<MediaItem>,
    initialIndex: Int,
    onClose: () -> Unit,
    onGetCollections: suspend (MediaItem, type: CollectionType) -> Map<Long, String>,
    onCollectionClick: (itemId: Long, type: CollectionType) -> Unit,
    onLoadMore: (() -> Unit)? = null,
    onUpdateSearchImage: ((uri: Uri) -> Unit)? = null,
    onSaveUpdatedItem: (MediaItem) -> Unit,
    maxSize: Int? = 1024
) {
    if (items.isEmpty()) return

    var showMenu by remember { mutableStateOf(false) }
    var isActionsVisible by remember { mutableStateOf(true) }
    var descriptionExpanded by remember { mutableStateOf(false) }
    var currentIndex by remember { mutableIntStateOf(initialIndex.coerceIn(0, items.lastIndex)) }
    val currentItem = items[currentIndex]

    var scale by remember(currentItem.id) { mutableFloatStateOf(1f) }
    var offset by remember(currentItem.id) { mutableStateOf(Offset.Zero) }

    val transformableState = rememberTransformableState { _, zoomChange, panChange, _ ->
        val newScale = (scale * zoomChange).coerceIn(1f, 5f)

        scale = newScale

        offset = if (newScale <= 1f) {
            Offset.Zero
        } else {
            offset + panChange
        }
    }

    val collectionCache = remember { mutableStateMapOf<Pair<Long, CollectionType>, Map<Long, String>>() }
    var tags by remember { mutableStateOf<Map<Long, String>>(emptyMap()) }
    var clusters by remember { mutableStateOf<Map<Long, String>>(emptyMap()) }

    val collections = buildList {
        tags.forEach { (id, name) -> add(Triple(id, name, CollectionType.TAG)) }
        clusters.forEach { (id, name) -> add(Triple(id, name, CollectionType.CLUSTER)) }
    }

    LaunchedEffect(currentItem.id) {
        val tagKey = currentItem.id to CollectionType.TAG
        val clusterKey = currentItem.id to CollectionType.CLUSTER

        tags = collectionCache.getOrPut(tagKey) { onGetCollections(currentItem, CollectionType.TAG) }
        clusters = collectionCache.getOrPut(clusterKey) { onGetCollections(currentItem, CollectionType.CLUSTER) }
    }

    fun showNextItem() {
        if (scale > 1f) return

        if (currentIndex < items.lastIndex) {
            currentIndex++

            if (currentIndex == items.lastIndex - 1) {
                onLoadMore?.invoke()
            }
        }
    }

    fun showPreviousItem() {
        if (scale > 1f) return

        if (currentIndex > 0) {
            currentIndex--
        }
    }

    Dialog(
        onDismissRequest = {
            if (descriptionExpanded) {
                descriptionExpanded = false
                isActionsVisible = true
            } else {
                onClose()
            }
        },
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .mediaViewerGestures(
                        gestureKey = currentItem.id,
                        isZoomed = scale > 1f,
                        onTap = { isActionsVisible = !isActionsVisible },
                        onDoubleTap = {
                            if (scale > 1f) {
                                scale = 1f
                                offset = Offset.Zero
                            } else {
                                scale = 3f
                                offset = Offset.Zero
                            }
                        },
                        onSwipeLeft = { showNextItem() },
                        onSwipeRight = { showPreviousItem() },
                        onSwipeUp = {
                            if (scale <= 1f) {
                                descriptionExpanded = true
                                isActionsVisible = false
                            }
                        },
                        onSwipeDown = {
                            if (scale <= 1f) {
                                descriptionExpanded = false
                                isActionsVisible = true
                            }
                        }
                    )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clipToBounds()
                        .transformable(
                            state = transformableState
                        )
                ) {
                    when (currentItem.type) {
                        MediaType.IMAGE -> {
                            ImageDisplay(
                                uri = currentItem.uri,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer {
                                        scaleX = scale
                                        scaleY = scale
                                        translationX = offset.x
                                        translationY = offset.y
                                    },
                                contentScale = ContentScale.FillWidth,
                                maxSize = maxSize,
                                mediaType = currentItem.type
                            )
                        }

                        MediaType.VIDEO -> {
                            VideoDisplay(
                                uri = currentItem.uri,
                                modifier = Modifier.fillMaxSize(),
                                onTap = { isActionsVisible = !isActionsVisible },
                                onSwipeLeft = { showNextItem() },
                                onSwipeRight = { showPreviousItem() },
                                onSwipeUp = {
                                    if (scale <= 1f) {
                                        descriptionExpanded = true
                                        isActionsVisible = false
                                    }
                                },
                                onSwipeDown = {
                                    if (scale <= 1f) {
                                        descriptionExpanded = false
                                        isActionsVisible = true
                                    }
                                }
                            )
                        }
                    }
                }

                if (descriptionExpanded) {
                    Box(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        key(currentItem.id, currentItem.type) {
                            MediaDetailsCard(
                                modifier = Modifier.fillMaxWidth(),
                                description = currentItem.description,
                                collections = collections,
                                onCollectionClick = { id, type -> onCollectionClick(id, type) },
                                onSave = { updated -> onSaveUpdatedItem(currentItem.copy(description = updated)) }
                            )
                        }
                    }
                }
            }

            MediaViewerActionRow(
                item = currentItem,
                onClose = onClose,
                onUpdateSearchImage = onUpdateSearchImage,
                toggleMenu = { showMenu = !showMenu },
                showMenu = showMenu,
                onViewDescription = { descriptionExpanded = true },
                isVisible = isActionsVisible
            )
        }
    }
}

fun Modifier.mediaViewerGestures(
    gestureKey: Long,
    isZoomed: Boolean,
    onTap: () -> Unit = {},
    onDoubleTap: () -> Unit = {},
    onSwipeLeft: () -> Unit = {},
    onSwipeRight: () -> Unit = {},
    onSwipeUp: () -> Unit = {},
    onSwipeDown: () -> Unit = {},
    threshold: Float = 100f
): Modifier {
    return this
        .pointerInput(gestureKey) {
            detectTapGestures(
                onTap = { onTap() },
                onDoubleTap = { onDoubleTap() }
            )
        }
        .pointerInput(isZoomed, threshold) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                val startPosition = down.position
                var endPosition = startPosition
                var hasMovedEnough = false
                var hasMultiplePointers = false

                while (true) {
                    val event = awaitPointerEvent()

                    if (event.changes.count { it.pressed } > 1) {
                        hasMultiplePointers = true
                    }

                    val change = event.changes.firstOrNull {
                        it.id == down.id
                    }

                    if (change != null) {
                        endPosition = change.position

                        val dx = endPosition.x - startPosition.x
                        val dy = endPosition.y - startPosition.y

                        if (
                            abs(dx) > threshold ||
                            abs(dy) > threshold
                        ) {
                            hasMovedEnough = true
                        }

                        if (change.changedToUpIgnoreConsumed()) {
                            break
                        }
                    }

                    if (event.changes.none { it.pressed }) {
                        break
                    }
                }

                if (
                    isZoomed ||
                    hasMultiplePointers ||
                    !hasMovedEnough
                ) {
                    return@awaitEachGesture
                }

                val dx = endPosition.x - startPosition.x
                val dy = endPosition.y - startPosition.y

                val absX = abs(dx)
                val absY = abs(dy)

                when {
                    absX > absY && absX > threshold -> {
                        if (dx < 0f) {
                            onSwipeLeft()
                        } else {
                            onSwipeRight()
                        }
                    }

                    absY > absX && absY > threshold -> {
                        if (dy < 0f) {
                            onSwipeUp()
                        } else {
                            onSwipeDown()
                        }
                    }
                }
            }
        }
}