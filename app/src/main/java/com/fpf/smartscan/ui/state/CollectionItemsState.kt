package com.fpf.smartscan.ui.state

import com.fpf.smartscan.media.MediaCollection
import com.fpf.smartscan.media.MediaItem
import com.fpf.smartscan.media.MediaType
import com.fpf.smartscan.search.SearchFilter
import com.fpf.smartscan.search.SortBy
import com.fpf.smartscan.ui.state.common.SelectionState

data class CollectionItemsState(
    val collection: MediaCollection? = null,
    val filter: SearchFilter = SearchFilter(),
    val sortBy: SortBy = SortBy.Date(),
    val loading: Boolean = false,
    val mediaToView: MediaItem? = null,
    val selection: SelectionState<MediaItem> = SelectionState()
)