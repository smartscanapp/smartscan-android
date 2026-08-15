package com.fpf.smartscan.ui.screens.concepts

import android.content.Context
import androidx.compose.ui.platform.Clipboard
import com.fpf.smartscan.core.concepts.Concept
import com.fpf.smartscan.core.media.MediaItem
import com.fpf.smartscan.core.media.MediaType
import com.fpf.smartscan.core.search.SortBy

sealed interface ConceptItemsAction {
    data class CopyMedia(val clipboard: Clipboard, val context: Context): ConceptItemsAction
    data class ShareMedia(val context: Context): ConceptItemsAction
    data class ToggleSelectedMedia(val item: MediaItem): ConceptItemsAction
    data class SetMediaToView(val item: MediaItem?): ConceptItemsAction
    data class SetConceptToView(val concept: Concept): ConceptItemsAction
    data class SetSelectAll(val selectAll: Boolean): ConceptItemsAction
    data class SetMediaTypeFilter(val mediaType: MediaType?): ConceptItemsAction
    data class SetShowHiddenFilter(val showHidden: Boolean): ConceptItemsAction
    data class ToggleHide(val item: MediaItem): ConceptItemsAction
    data object ResetFilters: ConceptItemsAction
    data class SetSortBy(val sortBy: SortBy): ConceptItemsAction
    data object ToggleSelectionMode: ConceptItemsAction
    data object ClearSelection: ConceptItemsAction
    data object ResetSelection: ConceptItemsAction
}