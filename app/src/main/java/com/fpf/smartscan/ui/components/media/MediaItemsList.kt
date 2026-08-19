package com.fpf.smartscan.ui.components.media

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.paging.compose.LazyPagingItems
import coil3.compose.AsyncImagePainter
import com.fpf.smartscan.core.media.MediaItem
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun MediaItemsList(
    isVisible: Boolean,
    items: LazyPagingItems<MediaItem>,
    selectedItems: Set<MediaItem>,
    excludedItems: Set<MediaItem>,
    selectAll: Boolean,
    onItemClick: (item: MediaItem) -> Unit,
    onLongItemClick: (item: MediaItem) -> Unit,
    onOffsetChange: (Int) -> Unit,
    numGridColumns: Int = 3,
    maxCollapsePx: Int = 0,
    isSelecting: Boolean = false,
    headerTitle: String? = null,
    onError: ((AsyncImagePainter.State.Error) -> Unit)? = null
) {
    if (!isVisible) return

    val scope = rememberCoroutineScope()
    val gridState = rememberLazyGridState()

    var showScrollToTop by remember { mutableStateOf(false) }
    var totalScrollPx by remember { mutableIntStateOf(0) }
    var initialVisibleItemCount by remember { mutableIntStateOf(0) }

    val connection = remember {
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

    LaunchedEffect(gridState) {
        var previousIndex = 0
        var previousOffset = 0

        snapshotFlow {
            gridState.firstVisibleItemIndex to gridState.firstVisibleItemScrollOffset
        }.collect { (index, offset) ->
            val visibleItemCount = gridState.layoutInfo.visibleItemsInfo.size
            val movedDown = index > previousIndex || (index == previousIndex && offset > previousOffset)
            val movedUp = index < previousIndex || (index == previousIndex && offset < previousOffset)

            if (initialVisibleItemCount == 0 && visibleItemCount > 0) {
                initialVisibleItemCount = visibleItemCount
            }

            val scrolledPastThreshold = initialVisibleItemCount > 0 && index >= 2 * initialVisibleItemCount

            showScrollToTop = when {
                index == 0 && offset == 0 -> false
                movedUp -> false
                movedDown && scrolledPastThreshold -> true
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
            item(span = { GridItemSpan(maxLineSpan) }) {
                headerTitle?.let {
                    Row(
                        modifier = Modifier.padding(vertical = 8.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(0.5.dp)
                                .border(
                                    1.dp,
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                        )
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(0.5.dp)
                                .border(
                                    1.dp,
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                        )
                    }
                }
            }

            items(
                count = items.itemCount,
                key = { index -> items[index]?.id ?: index }
            ) { index ->
                val item = items[index]

                if (item != null) {
                    MediaItemCard(
                        item=item,
                        onItemClick=onItemClick,
                        onLongItemClick = onLongItemClick,
                        isSelecting = isSelecting,
                        isChecked = { item in selectedItems || (selectAll && item !in excludedItems)},
                        onError=onError
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
            FloatingActionButton(
                onClick = {
                scope.launch {
                    showScrollToTop = false
                    onOffsetChange(0)
                    gridState.scrollToItem(0)
                }
            },
                elevation = FloatingActionButtonDefaults.elevation(
                    defaultElevation = 0.dp,
                    pressedElevation = 0.dp,
                    focusedElevation = 0.dp,
                    hoveredElevation = 0.dp
                )
            ) {
                Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Scroll to Top")
            }
        }
    }
}

