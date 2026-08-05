package com.fpf.smartscan.ui.components.media

import android.net.Uri
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateOffsetAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.Alignment
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
import kotlin.math.max

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
    var currentIndex by remember { mutableIntStateOf(initialIndex.coerceIn(0, items.lastIndex)) }
    var detailsExpanded by remember { mutableStateOf(false) }

    val currentItem = items[currentIndex]

    var currentItemWidth by remember(currentItem.id) { mutableIntStateOf(0) }
    var currentItemHeight by remember(currentItem.id) { mutableIntStateOf(0) }

    var targetScale by remember(currentItem.id) { mutableFloatStateOf(1f) }
    var targetOffset by remember(currentItem.id) { mutableStateOf(Offset.Zero) }

    val scale by animateFloatAsState(targetScale, label = "scale")
    val offset by animateOffsetAsState(targetOffset, label = "offset")

    val transitionProgress = remember { Animatable(0f) }

    LaunchedEffect(detailsExpanded) {
        transitionProgress.animateTo(
            targetValue = if (detailsExpanded) 1f else 0f,
            animationSpec = tween(
                durationMillis = 500,
                easing = FastOutSlowInEasing
            )
        )
    }

    val progress = transitionProgress.value
    val transformableState = rememberTransformableState { _, zoomChange, panChange, _ ->
        val newScale = (targetScale * zoomChange).coerceIn(1f, 5f)
        targetScale = newScale
        targetOffset = if (newScale <= 1f) Offset.Zero else targetOffset + panChange
    }

    val collectionCache = remember { mutableStateMapOf<Pair<Long, CollectionType>, Map<Long, String>>() }
    var tags by remember { mutableStateOf<Map<Long, String>>(emptyMap()) }
    var clusters by remember { mutableStateOf<Map<Long, String>>(emptyMap()) }

    val collections = buildList {
        tags.forEach { (id, name) -> add(Triple(id, name, CollectionType.TAG)) }
        clusters.forEach { (id, name) -> add(Triple(id, name, CollectionType.CLUSTER)) }
    }

    LaunchedEffect(currentItem.id) {
        tags = collectionCache.getOrPut(currentItem.id to CollectionType.TAG) { onGetCollections(currentItem, CollectionType.TAG) }
        clusters = collectionCache.getOrPut(currentItem.id to CollectionType.CLUSTER) { onGetCollections(currentItem, CollectionType.CLUSTER) }
        targetScale = 1f
        targetOffset = Offset.Zero
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

    fun calculateMediaScale(expanded: Boolean, width: Int, height: Int): Float {
        if (!expanded || width <= 0 || height <= 0) return 1f
        val aspect = width.toFloat() / height.toFloat()
        return max(aspect, 1f / aspect)
    }

    Dialog(
        onDismissRequest = {
            if (detailsExpanded) {
                detailsExpanded = false
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
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {

            Column(
                Modifier
                    .fillMaxSize()
                    .mediaViewerGestures(
                        gestureKey = currentItem.id,
                        isZoomed = scale > 1f,
                        onTap = { isActionsVisible = !isActionsVisible },
                        onDoubleTap = {
                            if (targetScale > 1f) {
                                targetScale = 1f
                                targetOffset = Offset.Zero
                            } else {
                                targetScale = 3f
                                targetOffset = Offset.Zero
                            }
                        },
                        onSwipeLeft = { showNextItem() },
                        onSwipeRight = { showPreviousItem() },
                        onSwipeUp = {
                            detailsExpanded = true
                            isActionsVisible = false
                        },

                        onSwipeDown = {
                            detailsExpanded = false
                            isActionsVisible = true
                        }
                    )
            ) {

                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clipToBounds()
                        .transformable(transformableState)
                ) {

                    val mediaHeight = maxHeight * (1f - (progress * 0.5f))
                    val detailsExpandedScale by animateFloatAsState(
                        targetValue = calculateMediaScale(expanded = detailsExpanded, width = currentItemWidth, height = currentItemHeight),
                        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
                        label = "detailsScale"
                    )

                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(mediaHeight)
                            .align(Alignment.TopCenter)
                            .clipToBounds()
                    ) {
                        when (currentItem.type) {
                            MediaType.IMAGE -> {
                                ImageDisplay(
                                    uri = currentItem.uri,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .graphicsLayer {
                                            scaleX = scale * detailsExpandedScale
                                            scaleY = scale * detailsExpandedScale
                                            translationX = offset.x
                                            translationY = offset.y
                                        },
                                    contentScale = ContentScale.FillWidth,
                                    maxSize = maxSize,
                                    mediaType = currentItem.type,
                                    onSizeChanged = { width, height ->
                                        currentItemWidth = width
                                        currentItemHeight = height
                                    }
                                )
                            }

                            MediaType.VIDEO -> {
                                VideoDisplay(
                                    uri = currentItem.uri,
                                    showControls = !detailsExpanded,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .graphicsLayer {
                                            scaleX = detailsExpandedScale
                                            scaleY = detailsExpandedScale
                                        },
                                    onTap = { isActionsVisible = !isActionsVisible },
                                    onSizeChanged = { width, height ->
                                        currentItemWidth = width
                                        currentItemHeight = height
                                    }
                                )
                            }
                        }
                    }


                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(maxHeight * 0.5f)
                            .align(Alignment.BottomCenter)
                            .graphicsLayer { translationY = maxHeight.toPx() * 0.5f * (1f - progress) }
                    ) {

                        if (progress > 0f) {
                            key(currentItem.id, currentItem.type) {
                                MediaDetailsCard(
                                    modifier = Modifier.fillMaxSize(),
                                    description = currentItem.description,
                                    collections = collections,
                                    onCollectionClick = { id, type -> onCollectionClick(id, type) },
                                    onSave = { updated -> onSaveUpdatedItem(currentItem.copy(description = updated)) }
                                )
                            }
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
                onViewDescription = { detailsExpanded = true },
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