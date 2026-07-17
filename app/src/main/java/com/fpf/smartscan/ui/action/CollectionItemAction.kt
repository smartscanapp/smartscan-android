package com.fpf.smartscan.ui.action

import android.content.Context
import androidx.compose.ui.platform.Clipboard
import com.fpf.smartscan.media.MediaCollection
import com.fpf.smartscan.media.MediaItem
import com.fpf.smartscan.media.MediaType

sealed interface CollectionItemAction {
    data class MoveMedia(val destinationCollection: MediaCollection): CollectionItemAction
    data class CopyMedia(val clipboard: Clipboard, val context: Context): CollectionItemAction
    data class ShareMedia(val context: Context): CollectionItemAction
    data class CreateNewCollectionAndMove(val newName: String): CollectionItemAction
    data class ToggleSelectedMedia(val item: MediaItem): CollectionItemAction
    data class SetMediaToView(val context: Context, val item: MediaItem?, val autoOpenInGallery: Boolean? = null, val isSelecting: Boolean = false): CollectionItemAction
    data class SetCollectionToView(val collection: MediaCollection): CollectionItemAction
    data class Tag(val tag: String): CollectionItemAction
    data class SetSelectAll(val selectAll: Boolean): CollectionItemAction
    data class SetMediaTypeFilter(val mediaType: MediaType?): CollectionItemAction

    data object RemoveMedia : CollectionItemAction
    data object ToggleSelectionMode: CollectionItemAction
    data object ClearSelection: CollectionItemAction
    data object ResetSelection: CollectionItemAction
}