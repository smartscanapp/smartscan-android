package com.fpf.smartscan.ui.components.concepts

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateSetOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaItem as ExoMediaItem
import androidx.paging.compose.LazyPagingItems
import coil3.compose.AsyncImagePainter
import com.fpf.smartscan.core.media.MediaItem
import com.fpf.smartscan.core.media.MediaType
import com.fpf.smartscan.ui.components.media.MediaItemFeedCard
import com.fpf.smartscan.ui.components.media.ImageDisplay
import com.fpf.smartscan.core.media.PlayerPool
import com.fpf.smartscan.ui.components.media.VideoDisplay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

@Composable
fun ConceptItemsList(
    isVisible: Boolean,
    playerPool: PlayerPool,
    items: LazyPagingItems<MediaItem>,
    onItemClick: (MediaItem) -> Unit,
    onShowItemMenu: (MediaItem) -> Unit,
    onOffsetChange: (Int) -> Unit,
    maxCollapsePx: Int = 0,
    onError: ((AsyncImagePainter.State.Error) -> Unit)? = null
) {
    if (!isVisible || playerPool.isEmpty()) return

    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val playbackPositions = remember { mutableStateMapOf<Long, Long>() }
    val manuallyPausedVideos = remember { mutableStateSetOf<Long>() }
    val programmaticPlaybackChanges = remember { mutableStateSetOf<Long>() }
    val minimumVisibilityFraction = 0.5f

    var showScrollToTop by remember { mutableStateOf(false) }
    var totalScrollPx by remember { mutableIntStateOf(0) }
    var playingVideoId by remember { mutableStateOf<Long?>(null) }
    var initialVisibleItemCount by remember { mutableIntStateOf(0) }

    val connection = remember(maxCollapsePx) {
        object : NestedScrollConnection {
            override fun onPreScroll(
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                val deltaPx = -available.y
                totalScrollPx = (totalScrollPx + deltaPx.roundToInt())
                    .coerceIn(0, maxCollapsePx)

                onOffsetChange(totalScrollPx)
                return Offset.Zero
            }
        }
    }

    LaunchedEffect(listState, items.itemCount) {
        var previousIndex = 0
        var previousOffset = 0

        snapshotFlow {
            val layoutInfo = listState.layoutInfo

            val visibleVideos = layoutInfo.visibleItemsInfo.mapNotNull { info ->
                if (info.index !in 0 until items.itemCount) return@mapNotNull null
                val item = items[info.index] ?: return@mapNotNull null
                if (item.type != MediaType.VIDEO) return@mapNotNull null

                val visibleStart = max(info.offset, layoutInfo.viewportStartOffset)
                val visibleEnd = min(info.offset + info.size, layoutInfo.viewportEndOffset)
                val visibleFraction =
                    (visibleEnd - visibleStart).coerceAtLeast(0).toFloat() / info.size

                item to visibleFraction
            }

            Triple(
                listState.firstVisibleItemIndex,
                listState.firstVisibleItemScrollOffset,
                visibleVideos
            )
        }
            .distinctUntilChanged()
            .collect { (index, offset, visibleVideos) ->
                val visibleItemCount = listState.layoutInfo.visibleItemsInfo.size
                val movedDown = index > previousIndex || (index == previousIndex && offset > previousOffset)
                val movedUp = index < previousIndex || (index == previousIndex && offset < previousOffset)

                if (initialVisibleItemCount == 0 && visibleItemCount > 0) {
                    initialVisibleItemCount = visibleItemCount
                }

                val scrolledPastThreshold = initialVisibleItemCount > 0 && index >= 5 * initialVisibleItemCount

                showScrollToTop = when {
                    index == 0 && offset == 0 -> false
                    movedUp -> false
                    movedDown && scrolledPastThreshold -> true
                    else -> showScrollToTop
                }

                previousIndex = index
                previousOffset = offset

                val visibleIds = visibleVideos.map { it.first.id }.toSet()

                playerPool.assignedIds
                    .filter { it !in visibleIds }
                    .toList()
                    .forEach { id ->
                        playerPool.get(id)?.let { player ->
                            playbackPositions[id] = player.currentPosition
                        }
                        playerPool.release(id)
                    }

                val currentVisibleFraction = visibleVideos.firstOrNull { it.first.id == playingVideoId }?.second ?: 0f
                val eligibleVideos = visibleVideos.filter { (_, fraction) ->
                    fraction >= minimumVisibilityFraction
                }
                val mostVisibleVideo = eligibleVideos.maxByOrNull { it.second }

                if (playingVideoId == null || currentVisibleFraction < minimumVisibilityFraction || (movedUp && mostVisibleVideo != null && mostVisibleVideo.first.id != playingVideoId)){
                    playingVideoId = mostVisibleVideo?.first?.id
                }

            visibleVideos.forEach { (video, visibilityFraction) ->
                playerPool.assign(video.id)?.let { player ->
                    if (player.currentMediaItem?.localConfiguration?.uri != video.uri) {
                        player.setMediaItem(ExoMediaItem.fromUri(video.uri))
                        player.prepare()
                        player.seekTo(playbackPositions[video.id] ?: 0L)
                    }
                    val shouldPlay = video.id == playingVideoId && visibilityFraction >= minimumVisibilityFraction && video.id !in manuallyPausedVideos
                    // Prevent autoplay playback changes from being treated as manual pauses
                    programmaticPlaybackChanges.add(video.id)

                    if (shouldPlay) {
                        player.play()
                    } else {
                        player.pause()
                    }
                    programmaticPlaybackChanges.remove(video.id)
                }
            }
        }
    }


    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(connection),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(0.dp)
        ) {
            items(
                count = items.itemCount,
                key = { index -> items[index]?.id ?: index }
            ) { index ->
                val item = items[index] ?: return@items

                MediaItemFeedCard(
                    item = item,
                    onItemClick = {
                        onItemClick(it)
                        if(item.type == MediaType.VIDEO){
                            playerPool.get(item.id)?.pause()
                        }
                    },
                    onShowItemMenu = onShowItemMenu,
                    content = {
                        when (item.type) {
                            MediaType.IMAGE -> {
                                ImageDisplay(
                                    size = 864,
                                    uri = item.uri,
                                    mediaType = item.type,
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier
                                        .clip(MaterialTheme.shapes.large)
                                        .background(Color.Black)
                                        .fillMaxWidth()
                                        .aspectRatio(1.25f),
                                    onError = onError
                                )
                            }

                            MediaType.VIDEO -> {
                                val player = playerPool.get(item.id)

                                if (player != null) {
                                    VideoDisplay(
                                        videoId = item.id,
                                        player = player,
                                        onPauseChanged = { paused ->
                                            if (item.id in programmaticPlaybackChanges) return@VideoDisplay

                                            if (paused) {
                                                manuallyPausedVideos.add(item.id)
                                            } else {
                                                manuallyPausedVideos.remove(item.id)
                                            }
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(MaterialTheme.shapes.large)
                                            .aspectRatio(1.25f)
                                    )
                                } else {
                                    ImageDisplay(
                                        size = 864,
                                        uri = item.uri,
                                        mediaType = MediaType.VIDEO,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .aspectRatio(1.25f),
                                        onError = onError
                                    )
                                }
                            }
                        }
                    }
                )
            }
        }

        AnimatedVisibility(
            visible = showScrollToTop,
            enter = fadeIn(animationSpec = tween(300)),
            exit = fadeOut(animationSpec = tween(300)),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            FloatingActionButton(
                onClick = {
                    scope.launch {
                        showScrollToTop = false
                        onOffsetChange(0)
                        listState.scrollToItem(0)
                    }
                },
                elevation = FloatingActionButtonDefaults.elevation(
                    defaultElevation = 0.dp,
                    pressedElevation = 0.dp,
                    focusedElevation = 0.dp,
                    hoveredElevation = 0.dp
                )
            ) {
                Icon(
                    Icons.Default.KeyboardArrowUp,
                    contentDescription = "Scroll to Top"
                )
            }
        }
    }
}