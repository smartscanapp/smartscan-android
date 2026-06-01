package com.fpf.smartscan.ui.action

import com.fpf.smartscan.media.CollectionType
import com.fpf.smartscan.media.MediaCollection

sealed interface CollectionAction {
    data class MergeCollections(val primaryCollectionName: String, val isNewMergedLabel: Boolean = false): CollectionAction
    data class RenameCollection(val newName: String): CollectionAction
    data class CreateNewTagAndTagClusters(val newName: String): CollectionAction
    data class ToggleSelectedCollection(val collection: MediaCollection): CollectionAction
    data class SetCollectionToView(val collection: MediaCollection?): CollectionAction
    data class SetCollectionType(val type: CollectionType) : CollectionAction
    data class SetSelectAll(val selectAll: Boolean): CollectionAction
    data object DeleteCollections : CollectionAction
    data object ToggleViewAllCollections: CollectionAction
    data object ToggleSelectionMode: CollectionAction
    data object ClearSelection: CollectionAction
    data object ResetSelection: CollectionAction
}