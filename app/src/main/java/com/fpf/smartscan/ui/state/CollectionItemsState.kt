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
    val selection: SelectionState<MediaItem> = SelectionState()
)