package com.fpf.smartscan.ui.components.concepts

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import com.fpf.smartscan.core.concepts.Concept
import kotlin.math.roundToInt

@Composable
fun ConceptsList(
    isVisible: Boolean,
    items: List<Concept>,
    onItemClick: (Concept) -> Unit,
    onItemLongClick: (Concept) -> Unit,
    selectedItems: Set<Concept> = emptySet(),
    excludedItems: Set<Concept> = emptySet(),
    onOffsetChange: ((Int) -> Unit)? = null,
    numGridColumns: Int = 3,
    maxCollapsePx: Int = 0,
    isSelecting: Boolean = false,
    selectAll: Boolean = false,

    ) {
    if (!isVisible) return

    val scope = rememberCoroutineScope()
    val gridState = rememberLazyStaggeredGridState()

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
        LazyVerticalStaggeredGrid (
            columns = StaggeredGridCells.Fixed(numGridColumns),
            state = gridState,
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(connection),
            contentPadding = PaddingValues(0.dp)
        ) {
            items(
                items = items,
                key = { it.id }
            ) { item ->
                ConceptCard(
                    item=item,
                    onItemClick=onItemClick,
                    onItemLongClick = onItemLongClick,
                    isSelecting = isSelecting,
                    isChecked = { item in selectedItems || (selectAll && item !in excludedItems)},
                )
            }
        }
    }
}
