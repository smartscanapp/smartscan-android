package com.fpf.smartscan.ui.screens.concepts


import android.app.Application
import android.content.ClipData
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.util.Log
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.platform.Clipboard
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.fpf.smartscan.R
import com.fpf.smartscan.concepts.Concept
import com.fpf.smartscan.constants.PrefsKeys
import com.fpf.smartscan.constants.PrefsNames
import com.fpf.smartscan.data.paging.ConceptPagingSource
import com.fpf.smartscan.data.mappers.toItem
import com.fpf.smartscan.data.media.MediaMetadataRepository
import com.fpf.smartscan.media.MediaItem
import com.fpf.smartscan.media.shareMediaMulti
import com.fpf.smartscan.search.SearchFilter
import com.fpf.smartscan.search.SortBy
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

    val sortByOptions: Map<SortBy, String>
        get()=  mapOf(
            SortBy.Date(ascending = true) to getApplication<Application>().getString(R.string.sort_date_asc_option),
            SortBy.Date(ascending = false) to getApplication<Application>().getString(R.string.sort_date_desc_option),
            SortBy.Similarity(ascending = true) to getApplication<Application>().getString(R.string.sort_similarity_asc_option),
            SortBy.Similarity(ascending = false) to getApplication<Application>().getString(R.string.sort_similarity_desc_option)
        )

    private val playbackPositions = mutableStateMapOf<Long, Long>()

    @OptIn(ExperimentalCoroutinesApi::class)
    val conceptItems = _state
        .map { Triple(it.filter, it.sortBy, it.concept) }
        .distinctUntilChanged()
        .flatMapLatest { (filters, sortBy, concept) ->

            Log.d(TAG, "Sort by: $sortBy")
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
                            sortBy=sortBy,
                            filter = filters,
                            conceptId = concept.id,
                            mediaMetadataRepository = mediaMetadataRepository,
                        )
                    }
                ).flow
            }
        }
        .cachedIn(viewModelScope)

    private val sharedPrefs by lazy { application.getSharedPreferences(PrefsNames.APP_PREFS, MODE_PRIVATE)}


    init {
        load()
    }

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
            is ConceptItemsAction.SetSortBy -> setSortBy(action.sortBy)
        }
    }

    fun getPlaybackPosition(id: Long): Long = playbackPositions[id] ?: 0L

    fun savePlaybackPosition(id: Long, position: Long) {
        playbackPositions[id] = position
    }

    private fun load(){
        _state.update { it.copy(sortBy = getSortByPref()) }
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

    private fun setSortBy(sortBy: SortBy) {
        _state.update { it.copy(sortBy = sortBy) }
        saveSortByPref(sortBy)
    }
    private fun saveSortByPref(sortBy: SortBy){
        val option =  sortByOptions.entries.find { it.key == sortBy }?.value?: sortByOptions.values.first()
        sharedPrefs.edit{
            putString(PrefsKeys.SORT_BY_CONCEPT_ITEMS, option)
        }
    }

    private fun getSortByPref(): SortBy{
        val sortByStr = sharedPrefs.getString(PrefsKeys.SORT_BY_CONCEPT_ITEMS, "")?: ""
        return sortByOptions.entries.find{ it.value == sortByStr}?.key?: SortBy.Date()
    }


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
