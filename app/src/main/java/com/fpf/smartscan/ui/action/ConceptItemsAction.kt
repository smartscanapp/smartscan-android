package com.fpf.smartscan.ui.action

import android.content.Context
import androidx.compose.ui.platform.Clipboard
import com.fpf.smartscan.concepts.Concept
import com.fpf.smartscan.media.MediaItem
import com.fpf.smartscan.media.MediaType

sealed interface ConceptItemsAction {
    data class CopyMedia(val clipboard: Clipboard, val context: Context): ConceptItemsAction
    data class ShareMedia(val context: Context): ConceptItemsAction
    data class ToggleSelectedMedia(val item: MediaItem): ConceptItemsAction
    data class SetMediaToView(val item: MediaItem?): ConceptItemsAction
    data class SetConceptToView(val concept: Concept): ConceptItemsAction
    data class SetSelectAll(val selectAll: Boolean): ConceptItemsAction
    data class SetMediaTypeFilter(val mediaType: MediaType?): ConceptItemsAction
    data object ToggleSelectionMode: ConceptItemsAction
    data object ClearSelection: ConceptItemsAction
    data object ResetSelection: ConceptItemsAction
}