package com.fpf.smartscan.ui.action

import com.fpf.smartscan.core.media.MediaItem
import com.fpf.smartscan.core.media.MediaType


sealed interface BinAction {
    data class ToggleSelectedMedia(val item: MediaItem): BinAction
    data class SetMediaToView(val item: MediaItem?): BinAction
    data class SetSelectAll(val selectAll: Boolean): BinAction
    data class SetMediaTypeFilter(val mediaType: MediaType?): BinAction
    data class Delete(val onDelete: (List<MediaItem>) -> Unit) : BinAction
    data class Restore(val onRestore: (List<MediaItem>) -> Unit) : BinAction
    data object ToggleSelectionMode: BinAction
    data object ClearSelection: BinAction
    data object ResetSelection: BinAction
}