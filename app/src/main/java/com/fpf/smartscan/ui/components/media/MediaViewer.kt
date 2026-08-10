package com.fpf.smartscan.ui.components.media

import android.net.Uri
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateOffsetAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.media3.common.MediaItem as ExoMediaItem
import androidx.media3.exoplayer.ExoPlayer
import com.fpf.smartscan.core.media.CollectionType
import com.fpf.smartscan.core.media.MediaItem
import com.fpf.smartscan.core.media.MediaType
import kotlin.math.max

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaViewer(
    items: List<MediaItem>,
    initialIndex: Int,
    onClose: () -> Unit,
    size: Int? = 1728,
    actionsEnabled: Boolean = true,
    onGetCollections: (suspend (MediaItem, type: CollectionType) -> Map<Long, String>)? = null,
    onCollectionClick: ((itemId: Long, type: CollectionType) -> Unit)? = null,
    onLoadMore: (() -> Unit)? = null,
    onUpdateSearchImage: ((uri: Uri) -> Unit)? = null,
    onSaveUpdatedItem: ((MediaItem) -> Unit)? = null,
) {
    if (items.isEmpty()) return

    val context = LocalContext.current

    var showMenu by remember { mutableStateOf(false) }
    var isActionsVisible by remember { mutableStateOf(true) }
    var detailsExpanded by remember { mutableStateOf(false) }
    var viewportSize by remember { mutableStateOf(IntSize.Zero) }

    // Media details
    var currentIndex by remember { mutableIntStateOf(initialIndex.coerceIn(0, items.lastIndex)) }
    val currentItem = items[currentIndex]
    var currentItemWidth by remember(currentItem.id) { mutableIntStateOf(0) }
    var currentItemHeight by remember(currentItem.id) { mutableIntStateOf(0) }
    val collectionCache = remember { mutableStateMapOf<Pair<Long, CollectionType>, Map<Long, String>>() }
    var tags by remember { mutableStateOf<Map<Long, String>>(emptyMap()) }
    var clusters by remember { mutableStateOf<Map<Long, String>>(emptyMap()) }
    val collections = buildList {
        tags.forEach { (id, name) -> add(Triple(id, name, CollectionType.TAG)) }
        clusters.forEach { (id, name) -> add(Triple(id, name, CollectionType.CLUSTER)) }
    }
    val videoPlayer = remember(context) { ExoPlayer.Builder(context).build() }

    // Pinch to zoom / scaling animations
    var targetScale by remember(currentItem.id) { mutableFloatStateOf(1f) }
    var targetOffset by remember(currentItem.id) { mutableStateOf(Offset.Zero) }
    val scale by animateFloatAsState(targetScale, label = "scale")
    val offset by animateOffsetAsState(targetOffset, label = "offset")
    val transitionProgress = remember { Animatable(0f) }
    val progress = transitionProgress.value
    val detailsExpandedScale by animateFloatAsState(
        targetValue = calculateMediaScale(expanded = detailsExpanded, width = currentItemWidth, height = currentItemHeight),
        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
        label = "detailsExpandedScale"
    )

    val transformableState = rememberTransformableState { _, zoomChange, panChange, _ ->
        val newScale = (targetScale * zoomChange).coerceIn(1f, 5f)
        targetScale = newScale

        if (newScale <= 1f || viewportSize.width == 0 || viewportSize.height == 0 || currentItemWidth == 0 || currentItemHeight == 0) {
            targetOffset = Offset.Zero
            return@rememberTransformableState
        }

        val viewportWidth = viewportSize.width.toFloat()
        val viewportHeight = viewportSize.height.toFloat()
        val imageAspect = currentItemWidth.toFloat() / currentItemHeight.toFloat()
        val fittedWidth: Float
        val fittedHeight: Float

        if (imageAspect > viewportWidth / viewportHeight) {
            fittedWidth = viewportWidth
            fittedHeight = viewportWidth / imageAspect
        } else {
            fittedWidth = viewportHeight * imageAspect
            fittedHeight = viewportHeight
        }

        val scale = newScale * detailsExpandedScale
        val maxX = ((fittedWidth * scale - viewportWidth) / 2f).coerceAtLeast(0f)
        val maxY = ((fittedHeight * scale - viewportHeight) / 2f).coerceAtLeast(0f)

        targetOffset = Offset(
            x = (targetOffset.x + panChange.x).coerceIn(-maxX, maxX),
            y = (targetOffset.y + panChange.y).coerceIn(-maxY, maxY)
        )
    }

    DisposableEffect(Unit) {
        onDispose { videoPlayer.release() }
    }

    LaunchedEffect(detailsExpanded) {
        transitionProgress.animateTo(
            targetValue = if (detailsExpanded) 1f else 0f,
            animationSpec = tween(
                durationMillis = 500,
                easing = FastOutSlowInEasing
            )
        )
    }

    LaunchedEffect(currentItem.uri) {
        if (currentItem.type == MediaType.VIDEO) {
            videoPlayer.setMediaItem(ExoMediaItem.fromUri(currentItem.uri))
            videoPlayer.prepare()
            videoPlayer.playWhenReady = true
        }else{
            videoPlayer.clearMediaItems()
        }
    }

    LaunchedEffect(currentItem.id) {
        onGetCollections?.let{
            tags = collectionCache.getOrPut(currentItem.id to CollectionType.TAG) { onGetCollections(currentItem, CollectionType.TAG) }
            clusters = collectionCache.getOrPut(currentItem.id to CollectionType.CLUSTER) { onGetCollections(currentItem, CollectionType.CLUSTER) }
        }
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
            dismissOnClickOutside = false,
            decorFitsSystemWindows = false,
            usePlatformDefaultWidth = false
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
                            if(detailsExpanded) return@mediaViewerGestures
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
                ) {

                    val mediaHeight = maxHeight * (1f - (progress * 0.5f))

                    Box(
                        Modifier
                            .transformable(transformableState)
                            .fillMaxWidth()
                            .height(mediaHeight)
                            .onSizeChanged { viewportSize = it }
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
                                    contentScale = ContentScale.Fit,
                                    maxSize = size,
                                    mediaType = currentItem.type,
                                    onSizeChanged = { width, height ->
                                        currentItemWidth = width
                                        currentItemHeight = height
                                    }
                                )
                            }

                            MediaType.VIDEO -> {
                                VideoDisplay(
                                    videoId = currentItem.id,
                                    player = videoPlayer,
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
                            .graphicsLayer {
                                translationY = maxHeight.toPx() * 0.5f * (1f - progress)
                            }
                    ) {

                        if (progress > 0f) {
                            key(currentItem.id, currentItem.type) {
                                MediaDetailsCard(
                                    modifier = Modifier.fillMaxSize(),
                                    description = currentItem.description,
                                    collections = collections,
                                    onCollectionClick = onCollectionClick,
                                    onSaveDescription = onSaveUpdatedItem?.let {
                                        { onSaveUpdatedItem(currentItem.copy(description = it)) }
                                    },
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
                isVisible = isActionsVisible,
                actionsEnabled = actionsEnabled
            )
        }
    }
}

fun calculateMediaScale(expanded: Boolean, width: Int, height: Int): Float {
    if (!expanded || width <= 0 || height <= 0) return 1f
    val aspect = width.toFloat() / height.toFloat()
    return max(aspect, 1f / aspect)
}