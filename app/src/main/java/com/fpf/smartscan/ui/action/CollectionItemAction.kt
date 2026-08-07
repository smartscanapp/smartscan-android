package com.fpf.smartscan.ui.action

import android.content.Context
import androidx.compose.ui.platform.Clipboard
import com.fpf.smartscan.core.media.MediaCollection
import com.fpf.smartscan.core.media.MediaItem
import com.fpf.smartscan.core.media.MediaType
import com.fpf.smartscan.core.search.SearchFilter
import com.fpf.smartscan.core.search.SortBy

sealed interface CollectionItemAction {
    data class MoveMedia(val destinationCollection: MediaCollection): CollectionItemAction
    data class CopyMedia(val clipboard: Clipboard, val context: Context): CollectionItemAction
    data class ShareMedia(val context: Context): CollectionItemAction
    data class CreateNewCollectionAndMove(val newName: String): CollectionItemAction
    data class ToggleSelectedMedia(val item: MediaItem): CollectionItemAction
    data class SetMediaToView(val item: MediaItem?): CollectionItemAction
    data class SetCollectionToView(val collection: MediaCollection): CollectionItemAction
    data class Tag(val tag: String): CollectionItemAction
    data class SetSelectAll(val selectAll: Boolean): CollectionItemAction
    data class SetMediaTypeFilter(val mediaType: MediaType?): CollectionItemAction
    data class SetDuplicateFilter(val duplicateFilter: Boolean?): CollectionItemAction
    data class SetSortBy(val sortBy: SortBy): CollectionItemAction
    data class Delete(val onDelete: (List<MediaItem>) -> Unit) : CollectionItemAction
    data object ResetFilters: CollectionItemAction
    data object RemoveTag : CollectionItemAction
    data object ToggleSelectionMode: CollectionItemAction
    data object ClearSelection: CollectionItemAction
    data object ResetSelection: CollectionItemAction
}