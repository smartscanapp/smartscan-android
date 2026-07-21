package com.fpf.smartscan.ui.screens.search

import com.fpf.smartscan.data.paging.SearchPagingSource
import android.app.Application
import android.content.ClipData
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.ui.platform.Clipboard
import kotlinx.coroutines.launch
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import coil3.compose.AsyncImagePainter
import kotlinx.coroutines.Dispatchers
import com.fpf.smartscan.R
import com.fpf.smartscan.data.clusters.ClusterCrossRefRepository
import com.fpf.smartscan.data.mappers.toItem
import com.fpf.smartscan.data.media.MediaMetadataRepository
import com.fpf.smartscan.events.SearchEvent
import com.fpf.smartscan.events.SearchEventType
import com.fpf.smartscan.media.MediaItem
import com.fpf.smartscan.media.MediaType
import com.fpf.smartscan.utils.canOpenUri
import com.fpf.smartscan.media.onMediaLoadingError
import com.fpf.smartscan.media.openImageInGallery
import com.fpf.smartscan.media.openVideoInGallery
import com.fpf.smartscan.search.SearchQuery
import com.fpf.smartscan.tag.TagManager
import com.fpf.smartscan.media.shareMediaMulti
import com.fpf.smartscan.search.SearchEngine
import com.fpf.smartscan.search.SearchFilter
import com.fpf.smartscan.search.SearchOptions
import com.fpf.smartscan.search.parseQuery
import com.fpf.smartscan.tag.Tag
import com.fpf.smartscan.ui.action.SearchAction
import com.fpf.smartscan.ui.state.SearchState
import com.fpf.smartscan.ui.state.common.SelectionState
import com.fpf.smartscan.ui.utils.SelectionUtils
import com.fpf.smartscansdk.core.embeddings.FileEmbeddingStore
import com.fpf.smartscansdk.ml.models.ModelAssetSource
import com.fpf.smartscansdk.ml.embeddings.clip.ClipImageEmbedder
import com.fpf.smartscansdk.ml.embeddings.clip.ClipTextEmbedder
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext


class SearchViewModel(
    application: Application,
    private val imageEmbedStore: FileEmbeddingStore,
    private val videoEmbedStore: FileEmbeddingStore,
    private val clusterEmbedStore: FileEmbeddingStore,
    private val tagManager: TagManager,
    private val clusterCrossRefRepository: ClusterCrossRefRepository,
    private val mediaMetadataRepository: MediaMetadataRepository
) : AndroidViewModel(application) {
    companion object {
        private const val TAG = "SearchViewModel"
    }


    private val textEmbedder  = ClipTextEmbedder(application, ModelAssetSource.Resource(R.raw.clip_text_encoder_quant), vocabSource = ModelAssetSource.Resource(R.raw.vocab), mergesSource = ModelAssetSource.Resource(R.raw.merges))

    private val imageEmbedder = ClipImageEmbedder(application, ModelAssetSource.Resource(R.raw.clip_image_encoder_quant))

    private val searchEngine = SearchEngine(
        application = getApplication(),
        dualEncoderVlm = Pair(textEmbedder, imageEmbedder),
        imageEmbedStore=imageEmbedStore,
        videoEmbedStore=videoEmbedStore,
        clusterEmbedStore=clusterEmbedStore,
        clusterCrossRefRepository = clusterCrossRefRepository
    )
    val allTags: StateFlow<List<Tag>> = tagManager.allTagsFlow.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val defaultMediaType = when{
        imageEmbedStore.exists && !videoEmbedStore.exists -> MediaType.IMAGE
        videoEmbedStore.exists && !imageEmbedStore.exists -> MediaType.VIDEO
        else -> MediaType.IMAGE
    }
    private val _state = MutableStateFlow(SearchState(filter = SearchFilter(mediaType = defaultMediaType)))
    val state: StateFlow<SearchState> = _state
    val searchFieldState: TextFieldState = TextFieldState()
    private var hasHandledExternalSearch = false

    private val _event = MutableSharedFlow<SearchEvent>()
    val event = _event.asSharedFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val searchResults = _state
        .map { Triple(it.filter, it.sortBy, it.resultIds) }
        .distinctUntilChanged()
        .flatMapLatest { (filters, sortBy, resultIds) ->

            if (resultIds.isEmpty()) {
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
                        SearchPagingSource(
                            filter = filters,
                            sortBy=sortBy,
                            resultIds = _state.value.resultIds.toList(),
                            mediaMetadataRepository = mediaMetadataRepository,
                        )
                    }
                ).flow
            }
        }
        .cachedIn(viewModelScope)


    fun onAction(action: SearchAction){
        when(action){
            is SearchAction.ClearDateFilters -> clearDateFilters()
            is SearchAction.CopyResult -> copyItem(action.clipboard, action.context)
            is SearchAction.SetQueryImageAndSearch -> {
                setQueryImage(action.image)
                search(action.searchOptions)
            }
            is SearchAction.RemoveUploadedImage -> removeUploadedImage()
            is SearchAction.SetEndDateFilter -> setEndDateFilter(action.date)
            is SearchAction.SetMediaTypeFilter -> setMediaType(action.mediaType)
            is SearchAction.SetStartDateFilter -> setStartDateFilter(action.date)
            is SearchAction.ShareResults -> shareItems(action.context)
            is SearchAction.TagItems -> tagItems(action.tag)
            is SearchAction.Search -> search(action.searchOptions)
            is SearchAction.ViewResult -> viewResult(action.context, action.item, action.autoOpenInGallery)
            is SearchAction.ToggleSelectedResult -> toggleSelectedResult(action.item)
            is SearchAction.Reset -> reset()
            is SearchAction.ClearResultView -> clearResultView()
            is SearchAction.SetSelectAll -> setSelectAll(action.selectAll)
            is SearchAction.ToggleSelectionMode -> toggleSelectionMode()
            is SearchAction.ResetSelection -> resetSelection()
            is SearchAction.ClearSelection -> clearSelection()
        }
    }

    private fun clearSelection() = _state.update{it.copy(selection = SelectionUtils.clearSelection(it.selection))}
    private fun resetSelection() = _state.update{it.copy(selection = SelectionUtils.resetSelection(it.selection))}
    private fun toggleSelectionMode() = _state.update { it.copy(selection = SelectionUtils.toggleSelectionMode(it.selection)) }

    private fun setMediaType(type: MediaType) {
        _state.update { it.copy(filter = it.filter.copy(mediaType = type)) }
        reset()
    }

    private fun reset(){
        _state.update{ it.copy(
            totalResults = 0,
            resultIds = emptySet(),
            selection = SelectionState(),
            resultToView = null,
            error = null,
            filter = it.filter.copy(tag = null),
            tagOnlySearch = false
        ) }
    }

    private fun search(searchOptions: SearchOptions){
        reset()
        _state.update { it.copy(loading = true) }

        viewModelScope.launch(Dispatchers.Default) {
            try {
                val query = searchFieldState.text.toString()
                val (tag, actualQuery) = parseQuery(query)
                tag?.let{ tag ->
                    val ids = getMediaMatchingTag(tag, _state.value.mediaType, state.value.filter.startDate, state.value.filter.endDate)
                    _state.update { it.copy(filter = it.filter.copy(tag=tag, ids = ids)) }
                    tagManager.updateLastUsage(tag)
                }
                val state = _state.value
                val tagOnlySearch = actualQuery.isBlank() && tag != null
                val queryResults =  when{
                    tagOnlySearch -> state.filter.ids.toList()
                    state.queryImage != null -> searchEngine.search(SearchQuery.ImageQuery(uri = state.queryImage, filter = state.filter, options = searchOptions))
                    searchFieldState.text.toString().isNotBlank() -> searchEngine.search(SearchQuery.TextQuery(text = searchFieldState.text.toString(), filter = state.filter, options = searchOptions))
                   else -> emptyList()
                }
                handleSearchResult(queryResults)
            }catch (e: Exception) {
                Log.e(TAG, "$e")
                _state.update{it.copy(error = getApplication<Application>().getString(R.string.search_error_unknown))}
            } finally {
                _state.update{it.copy(loading = false)}
            }
        }
    }

    private fun handleSearchResult(queryResults: List<Long>) {
        if (queryResults.isEmpty()) {
            _state.update{it.copy(error = getApplication<Application>().getString(R.string.search_error_no_results))}
        }else{
            _state.update{ it.copy(totalResults = queryResults.size, resultIds = queryResults.toSet())}
        }
    }

    fun externalSearch(intentSearchQuery: SearchQuery?, searchOptions: SearchOptions){
        if(intentSearchQuery == null || hasHandledExternalSearch) return

        when(intentSearchQuery) {
            is SearchQuery.ImageQuery -> {
                setMediaType(intentSearchQuery.filter.mediaType?: MediaType.IMAGE)
                setQueryImage(intentSearchQuery.uri)
                search(searchOptions)
                hasHandledExternalSearch = true
            }

            is SearchQuery.TextQuery -> {
                setMediaType(intentSearchQuery.filter.mediaType?: MediaType.IMAGE)
                searchFieldState.edit { replace(0, searchFieldState.text.length, intentSearchQuery.text) }
                search( searchOptions)
                hasHandledExternalSearch = true
            }
        }
    }

    private fun viewResult(context: Context, item: MediaItem, autoOpenInGallery: Boolean? = null){
        if(!canOpenUri(context, item.uri)){
            _state.update { currentState -> currentState.copy(resultIds = currentState.resultIds.filter{it != item.id}.toSet()) }
            return
        }

        if(autoOpenInGallery == true) {
            when(_state.value.mediaType){
                MediaType.IMAGE -> openImageInGallery(context, item.uri)
                MediaType.VIDEO -> openVideoInGallery(context, item.uri)
            }
        }else{
            _state.update{it.copy(resultToView = item)}
        }
    }

    private fun clearResultView() = _state.update {it.copy(resultToView = null)}

    private fun setQueryImage(uri: Uri?) = _state.update{it.copy(queryImage = uri)}

    private fun setStartDateFilter(date: Long?) = _state.update {it.copy(filter = it.filter.copy(startDate = date))}

    private fun setEndDateFilter(date: Long?) = _state.update {it.copy(filter = it.filter.copy(endDate = date))}

    private fun clearDateFilters() = _state.update {it.copy(filter = it.filter.copy(endDate = null, startDate = null))}

    private fun removeUploadedImage(){
        reset()
        setQueryImage(null)
    }

    private fun copyItem(clipboard: Clipboard, context: Context){
        viewModelScope.launch {
            val itemToCopy = getSelectedResults().first().uri
            clipboard.nativeClipboard.setPrimaryClip(ClipData.newUri(context.contentResolver, "smartscan_media", itemToCopy))
            resetSelection()
        }
    }

    private fun shareItems(context: Context){
        viewModelScope.launch {
            val selected = getSelectedResults()
            shareMediaMulti(context, selected.map{it.uri})
            resetSelection()
        }
    }

    private fun tagItems(tag: String){
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val selected = getSelectedResults()
                tagManager.tagItems(tag, selected)
                val message = if(selected.size == 1 ) "Tagged ${selected.size} item" else "Tagged ${selected.size} items"
                _event.emit(SearchEvent(SearchEventType.TAG, success = true, message = message))
            }catch (e: Exception){
                val message = "Error tagging results"
                Log.e(TAG, "$message: $e")
                _event.emit(SearchEvent(SearchEventType.TAG, success = false, message = message))
            }
        }
    }

    fun onErrorAsyncImage(error: AsyncImagePainter.State.Error){
        viewModelScope.launch (Dispatchers.IO){
            onMediaLoadingError(error,
                imageEmbedStore = imageEmbedStore,
                videoEmbedStore = videoEmbedStore,
                mediaMetadataRepository =mediaMetadataRepository
            )
        }
    }

    fun handleAutoCompletionCheck(query: CharSequence, substringEnd: Int, startWithHashtag: Boolean =  true): List<String>{
        return tagManager.checkAutoCompletion(query, substringEnd, allTags.value.map{it.name}, startWithHashtag)
    }

    fun onSelectAutoCompleteResult(tag: String){
        searchFieldState.edit { replace(0, searchFieldState.text.length, "#$tag ") }
    }

    private fun toggleSelectedResult(item: MediaItem){
        _state.update { it.copy(selection = SelectionUtils.toggleSelectedItem(it.selection, item, it.totalResults)) }
    }

    private fun setSelectAll(selectAll: Boolean) {
        _state.update { it.copy(selection = SelectionUtils.setSelectAll(it.selection, selectAll, it.totalResults))}
    }

    private suspend fun getSelectedResults(): Set<MediaItem> = SelectionUtils.getSelectedItems(_state.value.selection){getAllResults()}

    private suspend fun getAllResults(): MutableSet<MediaItem> {
        return withContext(Dispatchers.IO) {
            val mediaMetadataList = mediaMetadataRepository.getByIds(_state.value.resultIds.toList(), _state.value.mediaType)
            mediaMetadataList.map { it.toItem() }.toMutableSet()
        }
    }
    private suspend fun getMediaMatchingTag(tagName: String?, mediaType: MediaType, startDateFilter: Long? = null, endDateFilter: Long? = null): List<Long>{
        tagName?: return emptyList()
        val tag = tagManager.getTagByName(tagName)
        return if(endDateFilter != null || startDateFilter != null){
            tag?.let { tag-> mediaMetadataRepository.getByTag(tag.id, mediaType,startDateFilter, endDateFilter).map{it.id}  }?: emptyList()
        }else{
            tag?.let { tag-> mediaMetadataRepository.getByTag(tag.id, mediaType).map{it.id}  }?: emptyList()
        }
    }

    override fun onCleared() {
        textEmbedder.closeSession()
        imageEmbedder.closeSession()
        super.onCleared()
    }
}
