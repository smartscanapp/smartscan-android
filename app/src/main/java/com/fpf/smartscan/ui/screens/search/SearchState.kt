package com.fpf.smartscan.ui.screens.search

import android.net.Uri
import com.fpf.smartscan.core.media.MediaItem
import com.fpf.smartscan.core.media.MediaType
import com.fpf.smartscan.core.search.SearchFilter
import com.fpf.smartscan.core.search.SortBy
import com.fpf.smartscan.ui.shared.state.SelectionState

data class SearchState(
    val resultIds: Set<Long> = emptySet(),
    val queryImage: Uri? = null,
    val loading: Boolean = false,
    val error: String? = null,
    val resultToView: MediaItem? = null,
    val tagOnlySearch: Boolean = false,
    val selection: SelectionState<MediaItem> = SelectionState(),
    val filter: SearchFilter = SearchFilter(mediaType = MediaType.IMAGE),
    val sortBy: SortBy = SortBy.Date(),
    val recentSearches: Set<String> = emptySet(),
    val duplicateCount: Int = 0
){
    val mediaType: MediaType
        get()=filter.mediaType?: MediaType.IMAGE
    val totalResults: Int
        get()=when(filter.isDuplicate){
            true -> duplicateCount
            false -> (resultIds.size - duplicateCount).coerceAtLeast(0)
            else -> resultIds.size
        }
}