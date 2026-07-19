package com.fpf.smartscan.ui.components.collections

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
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
import com.fpf.smartscan.media.MediaItem
import com.fpf.smartscan.ui.components.media.MediaItemCard
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun CollectionItemsList(
    isVisible: Boolean,
    items: LazyPagingItems<MediaItem>,
    selectedItems: Set<MediaItem>,
    excludedItems: Set<MediaItem>,
    selectAll: Boolean,
    onItemClick: (item: MediaItem) -> Unit,
    onToggleSelected: (MediaItem) -> Unit,
    onToggleSelectionMode: () -> Unit,
    onOffsetChange: (Int) -> Unit,
    numGridColumns: Int = 3,
    maxCollapsePx: Int = 0,
    isSelecting: Boolean = false,
    onError: ((AsyncImagePainter.State.Error) -> Unit)? = null
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
            gridState.firstVisibleItemIndex to
                    gridState.firstVisibleItemScrollOffset
        }.collect { (index, offset) ->

            val movedDown =
                index > previousIndex || (index == previousIndex && offset > previousOffset)

            val movedUp =
                index < previousIndex || (index == previousIndex && offset < previousOffset)

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

            items(
                count = items.itemCount,
                key = { index -> items[index]?.id ?: index }
            ) { index ->
                val item = items[index]

                if (item != null) {
                    MediaItemCard(
                        item=item,
                        onItemClick=onItemClick,
                        onToggleSelected = onToggleSelected,
                        onToggleSelectionMode = onToggleSelectionMode,
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
            FloatingActionButton(onClick = {
                scope.launch {
                    showScrollToTop = false
                    onOffsetChange(0)
                    gridState.scrollToItem(0)
                }
            }) {
                Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Scroll to Top")
            }
        }
    }
}

