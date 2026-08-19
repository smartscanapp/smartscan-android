package com.fpf.smartscan.ui.screens.bin

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.fpf.smartscan.core.data.media.MediaMetadataRepository
import com.fpf.smartscan.core.data.mappers.toItem
import com.fpf.smartscan.core.data.paging.BinPagingSource
import com.fpf.smartscan.core.media.MediaItem
import com.fpf.smartscan.core.media.MediaType
import com.fpf.smartscan.ui.utils.SelectionUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.collections.map

class BinViewModel(
    application: Application,
    private val mediaMetadataRepository: MediaMetadataRepository,
) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "BinViewModel"
    }

    private val _state = MutableStateFlow(BinState())
    val state: StateFlow<BinState> = _state


    @OptIn(ExperimentalCoroutinesApi::class)
    val trashedItems = _state
        .map {it.trashedIds to it.filter}
        .distinctUntilChanged()
        .flatMapLatest { (trashedIds, filters) ->
            if (trashedIds.isEmpty()) {
                flowOf(PagingData.empty())
            } else {
                Pager(
                    config = PagingConfig(
                        pageSize = 50,
                        initialLoadSize = 50,
                        prefetchDistance = 25,
                        enablePlaceholders = false
                    ),
                    pagingSourceFactory = {
                        BinPagingSource(
                            filter = filters,
                            trashedIds = trashedIds,
                            mediaMetadataRepository = mediaMetadataRepository,
                        )
                    }
                ).flow
            }
        }
        .cachedIn(viewModelScope)


    init {
        viewModelScope.launch(Dispatchers.IO) {
            load()
        }
    }

    private suspend fun load(){
        val trashedIdsMap = mediaMetadataRepository.getIds(isTrashed = true)
        _state.update { it.copy(trashedIds = trashedIdsMap) }
        Log.d(TAG, "Loading complete results: ${trashedIdsMap.size}")
    }

    fun onAction(action: BinAction){
        when(action){
            is BinAction.SetMediaToView -> setMediaToView( action.item)
            is BinAction.ToggleSelectedMedia -> toggleSelectedItem(action.item)
            is BinAction.SetSelectAll -> setSelectAll(action.selectAll)
            is BinAction.ToggleSelectionMode -> toggleSelectionMode()
            is BinAction.ResetSelection -> resetSelection()
            is BinAction.ClearSelection -> clearSelection()
            is BinAction.SetMediaTypeFilter -> setMediaTypeFilter(action.mediaType)
            is BinAction.Delete -> deleteFromDevice(action.onDelete)
            is BinAction.Restore -> restore(action.onRestore)
        }
    }

    private fun clearSelection() = _state.update{it.copy(selection = SelectionUtils.clearSelection(it.selection))}
    private fun resetSelection() = _state.update{it.copy(selection = SelectionUtils.resetSelection(it.selection))}
    private fun toggleSelectionMode() = _state.update { it.copy(selection = SelectionUtils.toggleSelectionMode(it.selection)) }

    private fun deleteFromDevice(onDelete: (List<MediaItem>) -> Unit){
        viewModelScope.launch{
            val items = withContext(Dispatchers.IO) {
                getSelectedItems().toList()
            }
            onDelete(items)
            resetSelection()
        }
    }

    private fun restore(onRestore: (List<MediaItem>) -> Unit){
        viewModelScope.launch{
            val items = withContext(Dispatchers.IO) {
                getSelectedItems().toList()
            }
            onRestore(items)
            resetSelection()
        }
    }
    private fun toggleSelectedItem(item: MediaItem){
        _state.update {
            it.copy(selection = SelectionUtils.toggleSelectedItem(it.selection, item, _state.value.trashedIds.size))
        }
    }

    private fun setSelectAll(selectAll: Boolean) = _state.update { it.copy(selection = SelectionUtils.setSelectAll(it.selection, selectAll, _state.value.trashedIds.size))}

    private suspend fun getSelectedItems(): Set<MediaItem> = SelectionUtils.getSelectedItems(_state.value.selection){getAllTrashedMediaItems()}

    private fun setMediaToView(item: MediaItem?) = _state.update { it.copy(mediaToView =item) }

    private fun setMediaTypeFilter(mediaType: MediaType?) = _state.update { it.copy(filter =it.filter.copy(mediaType=mediaType)) }

    private suspend fun getAllTrashedMediaItems(): MutableSet<MediaItem>{
        return mediaMetadataRepository.get(isTrashed = true).map{it.toItem()}.toMutableSet()
    }
}
