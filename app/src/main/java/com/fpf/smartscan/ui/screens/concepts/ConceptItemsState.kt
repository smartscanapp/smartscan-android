package com.fpf.smartscan.ui.screens.concepts

import com.fpf.smartscan.core.concepts.Concept
import com.fpf.smartscan.core.media.MediaFilter
import com.fpf.smartscan.core.media.MediaItem
import com.fpf.smartscan.core.search.SortBy
import com.fpf.smartscan.ui.shared.state.SelectionState

data class ConceptItemsState(
    val concept: Concept? = null,
    val filter: MediaFilter = MediaFilter(),
    val sortBy: SortBy = SortBy.Date(),
    val loading: Boolean = false,
    val mediaToView: MediaItem? = null,
    val selection: SelectionState<MediaItem> = SelectionState()
)