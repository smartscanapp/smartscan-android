package com.fpf.smartscan.ui.state

import com.fpf.smartscan.core.media.MediaCollection
import com.fpf.smartscan.core.media.MediaFilter
import com.fpf.smartscan.core.media.MediaItem
import com.fpf.smartscan.core.search.SearchFilter
import com.fpf.smartscan.core.search.SortBy
import com.fpf.smartscan.ui.state.common.SelectionState

data class CollectionItemsState(
    val collection: MediaCollection? = null,
    val filter: MediaFilter = MediaFilter(),
    val sortBy: SortBy = SortBy.Date(),
    val loading: Boolean = false,
    val mediaToView: MediaItem? = null,
    val selection: SelectionState<MediaItem> = SelectionState(),
    val duplicateCount: Int = 0
){
    val collectionSize: Int
        get() = collection?.size?: 0

    val totalItems: Int
        get() = when(filter.isDuplicate){
            true -> duplicateCount
            false -> (collectionSize - duplicateCount).coerceAtLeast(0)
            else -> collectionSize
        }
}