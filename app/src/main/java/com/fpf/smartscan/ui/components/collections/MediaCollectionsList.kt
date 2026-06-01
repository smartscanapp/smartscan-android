package com.fpf.smartscan.ui.components.collections

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.fpf.smartscan.media.MediaCollection
import com.fpf.smartscan.media.MediaType
import com.fpf.smartscan.ui.components.common.CircularCheckbox
import com.fpf.smartscan.ui.components.media.ImageDisplay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun MediaCollectionsList(
    isVisible: Boolean,
    items: List<MediaCollection>,
    onItemClick: (MediaCollection) -> Unit,
    selectedItems: Set<MediaCollection> = emptySet(),
    excludedItems: Set<MediaCollection> = emptySet(),
    onToggleSelected: ((MediaCollection) -> Unit)? = null,
    onToggleSelectionMode: (() -> Unit)? = null,
    onOffsetChange: ((Int) -> Unit)? = null,
    numGridColumns: Int = 3,
    maxCollapsePx: Int = 0,
    isSelecting: Boolean = false,
    selectAll: Boolean = false,

    ) {
    if (!isVisible) return

    val scope = rememberCoroutineScope()
    val gridState = rememberLazyGridState()

    var showScrollToTop by remember { mutableStateOf(false) }
    var totalScrollPx by remember { mutableIntStateOf(0) }

    val connection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                val deltaPx = -available.y
                totalScrollPx = (totalScrollPx + deltaPx.roundToInt()).coerceIn(0, maxCollapsePx)
                onOffsetChange?.invoke(totalScrollPx)
                return Offset.Zero
            }
        }
    }

    LaunchedEffect(gridState) {
        var previousIndex = 0
        var previousOffset = 0

        snapshotFlow {
            gridState.firstVisibleItemIndex to gridState.firstVisibleItemScrollOffset
        }.collect { (index, offset) ->

            val movedDown = index > previousIndex || (index == previousIndex && offset > previousOffset)
            val movedUp = index < previousIndex || (index == previousIndex && offset < previousOffset)

            showScrollToTop = when {
                index == 0 && offset == 0 -> false
                movedUp -> false
                movedDown -> true
                else -> showScrollToTop
            }

            previousIndex = index
            previousOffset = offset
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyVerticalGrid(
            state = gridState,
            columns = GridCells.Fixed(numGridColumns),
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(connection),
            contentPadding = PaddingValues(0.dp)
        ) {
            items(items, key = { it.id }) { item ->
                val shape = RoundedCornerShape(12.dp)

                Column{
                    Box(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .padding(4.dp)
                            .clip(shape)
                            .border(1.dp, Color.Gray.copy(alpha = 0.2f), shape)
                            .combinedClickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() },
                                onClick = {
                                    if (isSelecting) onToggleSelected?.invoke(item) else onItemClick(
                                        item
                                    )
                                },
                                onLongClick = {
                                    if (!isSelecting) {
                                        onToggleSelectionMode?.invoke()
                                        onToggleSelected?.invoke(item)
                                    }
                                }
                            )
                    ) {
                        ImageDisplay(
                            uri = item.thumbNail,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                            mediaType = MediaType.IMAGE
                        )

                        if (isSelecting) {
                            CircularCheckbox(
                                checked = item in selectedItems || (selectAll && item !in excludedItems),
                                onCheckedChange = { onToggleSelected?.invoke(item) },
                                modifier = Modifier
                                    .offset(x = 8.dp, y = 8.dp)
                                    .align(Alignment.TopStart),
                            )
                        }
                    }
                    Text(
                        text = item.name.replaceFirstChar { char -> char.uppercase() },
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding( start = 2.dp),
                        color= MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = item.size.toString(),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(bottom = 4.dp, start = 2.dp),
                        color= MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
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
            FloatingActionButton(onClick = {
                scope.launch {
                    showScrollToTop = false
                    onOffsetChange?.invoke(0)
                    gridState.scrollToItem(0)
                }
            }) {
                Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Scroll to Top")
            }
        }
    }
}
