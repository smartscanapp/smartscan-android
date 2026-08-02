package com.fpf.smartscan.ui.state

import android.net.Uri
import com.fpf.smartscan.core.media.MediaItem
import com.fpf.smartscan.core.media.MediaType
import com.fpf.smartscan.core.search.SearchFilter
import com.fpf.smartscan.core.search.SortBy
import com.fpf.smartscan.ui.state.common.SelectionState

data class SearchState(
    val resultIds: Set<Long> = emptySet(),
    val totalResults: Int = 0,
    val queryImage: Uri? = null,
    val loading: Boolean = false,
    val error: String? = null,
    val resultToView: MediaItem? = null,
    val tagOnlySearch: Boolean = false,
    val selection: SelectionState<MediaItem> = SelectionState(),
    val filter: SearchFilter = SearchFilter(mediaType = MediaType.IMAGE),
    val sortBy: SortBy = SortBy.Date(),
    val recentSearches: Set<String> = emptySet()
){
    val mediaType: MediaType
        get()=filter.mediaType?: MediaType.IMAGE
}