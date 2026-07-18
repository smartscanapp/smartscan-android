package com.fpf.smartscan.ui.screens.concepts


import android.app.Application
import android.content.ClipData
import android.content.Context
import androidx.compose.ui.platform.Clipboard
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.fpf.smartscan.concepts.Concept
import com.fpf.smartscan.data.concepts.ConceptPagingSource
import com.fpf.smartscan.data.mappers.toItem
import com.fpf.smartscan.data.metadata.MediaMetadataRepository
import com.fpf.smartscan.media.MediaItem
import com.fpf.smartscan.media.shareMediaMulti
import com.fpf.smartscan.search.SearchFilter
import com.fpf.smartscan.ui.action.ConceptItemsAction
import com.fpf.smartscan.ui.state.ConceptItemsState
import com.fpf.smartscan.ui.utils.SelectionUtils
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.collections.map

class ConceptItemsViewModel(
    application: Application,
    private val mediaMetadataRepository: MediaMetadataRepository,
) : AndroidViewModel(application) {
    companion object {
        private const val TAG = "ConceptItemsViewModel"
    }

    private val _state = MutableStateFlow(ConceptItemsState())
    val state: StateFlow<ConceptItemsState> = _state

    @OptIn(ExperimentalCoroutinesApi::class)
    val conceptItems = _state
        .map { it.filter to it.concept }
        .distinctUntilChanged()
        .flatMapLatest { (filters, concept) ->

            if (concept?.id == null) {
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
                        ConceptPagingSource(
                            filter = filters,
                            conceptId = concept.id,
                            mediaMetadataRepository = mediaMetadataRepository,
                        )
                    }
                ).flow
            }
        }
        .cachedIn(viewModelScope)
    

    fun onAction(action: ConceptItemsAction){
        when(action){
            is ConceptItemsAction.CopyMedia -> copyItem(action.clipboard, action.context)
            is ConceptItemsAction.SetMediaToView -> setMediaToView(action.item)
            is ConceptItemsAction.ShareMedia -> shareItems(action.context)
            is ConceptItemsAction.ToggleSelectedMedia -> toggleSelectedItem(action.item)
            is ConceptItemsAction.SetConceptToView -> setConcept(action.concept)
            is ConceptItemsAction.SetSelectAll -> setSelectAll(action.selectAll)
            is ConceptItemsAction.ToggleSelectionMode -> toggleSelectionMode()
            is ConceptItemsAction.ResetSelection -> resetSelection()
            is ConceptItemsAction.ClearSelection -> clearSelection()
            is ConceptItemsAction.SetFilter -> setFilter(action.filter)
        }
    }

    private fun clearSelection() = _state.update{it.copy(selection = SelectionUtils.clearSelection(it.selection))}
    private fun resetSelection() = _state.update{it.copy(selection = SelectionUtils.resetSelection(it.selection))}
    private fun toggleSelectionMode() = _state.update { it.copy(selection = SelectionUtils.toggleSelectionMode(it.selection)) }

    private fun copyItem(clipboard: Clipboard, context: Context){
        viewModelScope.launch {
            val itemToCopy = getSelectedItems().first().uri
            clipboard.nativeClipboard.setPrimaryClip(ClipData.newUri(context.contentResolver, "smartscan_media", itemToCopy))
//            _event.emit(CollectionItemEvent(CollectionItemEventType.COPY, success = true))
            resetSelection()
        }
    }

    private fun shareItems(context: Context){
        viewModelScope.launch {
            val items = getSelectedItems()
            shareMediaMulti(context, items.map{it.uri})
//            _event.emit(CollectionItemEvent(CollectionItemEventType.SHARE, success = true))
            resetSelection()
        }
    }

    private fun toggleSelectedItem(item: MediaItem){
        _state.update {
            val concept = it.concept ?: return
            it.copy(selection = SelectionUtils.toggleSelectedItem(it.selection, item, concept.size))
        }
    }

    private fun setSelectAll(selectAll: Boolean) {
        val currentState = _state.value
        val concept = currentState.concept?: return
        _state.update { it.copy(selection = SelectionUtils.setSelectAll(it.selection, selectAll, concept.size))}

    }

    private suspend fun getSelectedItems(): Set<MediaItem> = SelectionUtils.getSelectedItems(_state.value.selection){getAllItemsInConcept()}

    private suspend fun getAllItemsInConcept(): MutableSet<MediaItem> {
        val currentState = state.value
        val concept = currentState.concept ?: return mutableSetOf()
        return mediaMetadataRepository.getByConceptSortedByDate(concept.id).map { it.toItem() }.toMutableSet()
    }

    private fun setConcept(concept: Concept?) = _state.update { it.copy(concept=concept) }

    private fun setMediaToView(item: MediaItem?) = _state.update { it.copy(mediaToView =item) }
    private fun setFilter(filter: SearchFilter) = _state.update { it.copy(filter = filter) }


    // TODO: update this to be event based
//    fun onErrorAsyncImage(error: AsyncImagePainter.State.Error){
//        viewModelScope.launch (Dispatchers.IO){
//            onMediaLoadingError(error,
//                imageEmbedStore = imageEmbedStore,
//                videoEmbedStore = videoEmbedStore,
//                mediaMetadataRepository =mediaMetadataRepository
//            )
//        }
//    }
}
