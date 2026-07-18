package com.fpf.smartscan.ui.state

import com.fpf.smartscan.concepts.Concept
import com.fpf.smartscan.media.MediaItem
import com.fpf.smartscan.media.MediaType
import com.fpf.smartscan.search.SearchFilter
import com.fpf.smartscan.search.SortBy
import com.fpf.smartscan.ui.state.common.SelectionState

data class ConceptItemsState(
    val concept: Concept? = null,
    val filter: SearchFilter = SearchFilter(),
    val sortBy: SortBy = SortBy.Date(),
    val loading: Boolean = false,
    val mediaToView: MediaItem? = null,
    val selection: SelectionState<MediaItem> = SelectionState()
)