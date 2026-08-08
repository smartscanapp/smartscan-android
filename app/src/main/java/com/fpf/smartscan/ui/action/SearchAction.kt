package com.fpf.smartscan.ui.action

import android.content.Context
import android.net.Uri
import androidx.compose.ui.platform.Clipboard
import com.fpf.smartscan.core.media.MediaItem
import com.fpf.smartscan.core.media.MediaType

sealed interface SearchAction {
    data object Search: SearchAction
    data class SetQueryImageAndSearch(val image: Uri): SearchAction
    data class ViewResult(val item: MediaItem): SearchAction
    data class ToggleSelectedResult(val item: MediaItem): SearchAction
    data class TagItems(val tag: String): SearchAction
    data class SetStartDateFilter(val date: Long?): SearchAction
    data class SetEndDateFilter(val date: Long?): SearchAction
    data class SetMediaTypeFilter(val mediaType: MediaType): SearchAction
    data class SetDuplicateFilter(val duplicateFilter: Boolean?): SearchAction
    data class CopyResult(val clipboard: Clipboard, val context: Context): SearchAction
    data class ShareResults(val context: Context): SearchAction
    data class SetSelectAll(val selectAll: Boolean): SearchAction
    data class Delete(val onDelete: (List<MediaItem>) -> Unit) : SearchAction
    data class RemoveRecentSearch(val query: String): SearchAction
    data object RemoveUploadedImage: SearchAction
    data object ClearRecentSearches: SearchAction
    data object ResetFilters: SearchAction
    data object ClearStartDateFilter: SearchAction
    data object ClearEndDateFilter: SearchAction
    data object Reset: SearchAction
    data object ClearResultView: SearchAction
    data object ToggleSelectionMode: SearchAction
    data object ClearSelection: SearchAction
    data object ResetSelection: SearchAction
}