package com.fpf.smartscan.ui.screens.search

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
import coil3.compose.AsyncImagePainter
import kotlinx.coroutines.Dispatchers
import com.fpf.smartscan.R
import com.fpf.smartscan.data.clusters.ClusterCrossRefRepository
import com.fpf.smartscan.data.clusters.ClusterMetadataRepository
import com.fpf.smartscan.data.metadata.MediaMetadataRepository
import com.fpf.smartscan.data.tags.TagCrossRefRepository
import com.fpf.smartscan.data.tags.TagRepository
import com.fpf.smartscan.data.tags.TagEntity
import com.fpf.smartscan.events.SearchEvent
import com.fpf.smartscan.events.SearchEventType
import com.fpf.smartscan.media.MediaItem
import com.fpf.smartscan.media.MediaStoreHelper
import com.fpf.smartscan.media.MediaType
import com.fpf.smartscan.utils.canOpenUri
import com.fpf.smartscan.media.onMediaLoadingError
import com.fpf.smartscan.media.openImageInGallery
import com.fpf.smartscan.media.openVideoInGallery
import com.fpf.smartscan.media.removeStaleMedia
import com.fpf.smartscan.media.toMediaItem
import com.fpf.smartscan.search.SearchQuery
import com.fpf.smartscan.tag.TagManager
import com.fpf.smartscan.media.mediaIdToUri
import com.fpf.smartscan.media.shareMediaMulti
import com.fpf.smartscan.media.toItem
import com.fpf.smartscan.search.dedupe
import com.fpf.smartscan.search.getPaginatedResult
import com.fpf.smartscan.search.parseQuery
import com.fpf.smartscan.search.rerankItems
import com.fpf.smartscan.tag.Tag
import com.fpf.smartscan.ui.action.SearchAction
import com.fpf.smartscan.ui.state.SearchState
import com.fpf.smartscan.ui.state.common.SelectionState
import com.fpf.smartscan.ui.utils.SelectionUtils
import com.fpf.smartscansdk.core.embeddings.FileEmbeddingStore
import com.fpf.smartscansdk.core.embeddings.QueryResult
import com.fpf.smartscansdk.core.embeddings.toQInt8Embed
import com.fpf.smartscansdk.core.media.getBitmapFromUri
import com.fpf.smartscansdk.ml.models.ModelAssetSource
import com.fpf.smartscansdk.ml.embeddings.clip.ClipImageEmbedder
import com.fpf.smartscansdk.ml.embeddings.clip.ClipImageEmbedder.Companion.IMAGE_SIZE_X
import com.fpf.smartscansdk.ml.embeddings.clip.ClipTextEmbedder
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean


class SearchViewModel(
    application: Application,
    private val imageEmbedStore: FileEmbeddingStore,
    private val videoEmbedStore: FileEmbeddingStore,
    private val clusterEmbedStore: FileEmbeddingStore,
    private val tagRepository: TagRepository,
    private val tagCrossRefRepository: TagCrossRefRepository,
    private val clusterCrossRefRepository: ClusterCrossRefRepository,
    private val clusterMetadataRepository: ClusterMetadataRepository,
    private val mediaMetadataRepository: MediaMetadataRepository
) : AndroidViewModel(application) {
    companion object {
        private const val TAG = "SearchViewModel"
        const val RESULTS_BATCH_SIZE = 36
        private const val MODEL_SHUTDOWN_DURATION_THRESHOLD = 60_000L
        private const val DEDUPE_THRESHOLD = 0.95f
        private const val TEXT_QUERY_THRESHOLD = 0.2f
        private const val IMAGE_QUERY_THRESHOLD = 0.5f

    }

    private val textEmbedder  = ClipTextEmbedder(application, ModelAssetSource.Resource(R.raw.clip_text_encoder_quant), vocabSource = ModelAssetSource.Resource(R.raw.vocab), mergesSource = ModelAssetSource.Resource(R.raw.merges))

    private val imageEmbedder = ClipImageEmbedder(application, ModelAssetSource.Resource(R.raw.clip_image_encoder_quant))

    val tagManager = TagManager(
        tagRepository=tagRepository,
        tagCrossRefRepository=tagCrossRefRepository,
        mediaMetadataRepository = mediaMetadataRepository,
        )

    val allTags: StateFlow<List<Tag>> = tagRepository.allTags.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val defaultMediaType = when{
        imageEmbedStore.exists && !videoEmbedStore.exists -> MediaType.IMAGE
        videoEmbedStore.exists && !imageEmbedStore.exists -> MediaType.VIDEO
        else -> MediaType.IMAGE
    }
    private val _state = MutableStateFlow(SearchState(mediaType = defaultMediaType))
    val state: StateFlow<SearchState> = _state
    val searchFieldState: TextFieldState = TextFieldState()
    private val isLoadingMoreSearchResults = AtomicBoolean(false)

    private var hasHandledExternalSearch = false

    private var cachedIds= mutableListOf<Long>()

    private val _event = MutableSharedFlow<SearchEvent>()
    val event = _event.asSharedFlow()

    fun onAction(action: SearchAction){
        when(action){
            is SearchAction.ClearDateFilters -> clearDateFilters()
            is SearchAction.CopyResult -> copyItem(action.clipboard, action.context)
            is SearchAction.SetQueryImageAndSearch -> {
                setQueryImage(action.image)
                search(action.strictness, action.dedupeEnabled)
            }
            is SearchAction.RemoveUploadedImage -> removeUploadedImage()
            is SearchAction.SetEndDateFilter -> setEndDateFilter(action.date)
            is SearchAction.SetMediaTypeFilter -> setMediaType(action.mediaType)
            is SearchAction.SetStartDateFilter -> setStartDateFilter(action.date)
            is SearchAction.ShareResults -> shareItems(action.context)
            is SearchAction.TagItems -> tagItems(action.tag)
            is SearchAction.Search -> search(action.strictness, action.dedupeEnabled)
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
        _state.update { it.copy(mediaType = type) }
        reset()
    }

    private fun reset(){
        cachedIds = mutableListOf() // clear on new search
        _state.update{ it.copy(
            totalResults = 0,
            searchResults = emptyList(),
            selection = SelectionState(),
            resultToView = null,
            error = null,
            tagFilter = null,
            tagOnlySearch = false
        ) }
    }

    private fun search(strictness: Float, dedupeEnabled: Boolean){
        reset()
        val store = getStore()
        if(!store.exists) {
            _state.update{ currentState -> currentState.copy(error = getApplication<Application>().getString(R.string.search_error_not_indexed))}
            return
        }
        _state.update { it.copy(loading = true) }

        viewModelScope.launch(Dispatchers.Default) {
            try {
                val state = _state.value
                val queryResults = if (state.queryImage != null) {
                   imageSearch(store, strictness, startDate = state.startDateFilter, endDate = state.endDateFilter)
                } else {
                    textSearch(store, strictness, startDate = state.startDateFilter, endDate = state.endDateFilter)
                }
                handleSearchResult(queryResults, store, dedupeEnabled)
            }catch (e: Exception) {
                Log.e(TAG, "$e")
                _state.update{it.copy(error = getApplication<Application>().getString(R.string.search_error_unknown))}
            } finally {
                _state.update{it.copy(loading = false)}
            }
        }
    }

    private suspend fun textSearch(store: FileEmbeddingStore, strictness: Float, startDate: Long? = null, endDate: Long? = null): List<Long> {
        val query = searchFieldState.text.toString()
        if (query.isBlank()) {
            _state.update{currentState -> currentState.copy(error = getApplication<Application>().getString(R.string.search_error_empty_query))}
            return emptyList()
        }
        val (tag, actualQuery) = parseQuery(query)
        tag?.let{ tag ->
            _state.update { currentState -> currentState.copy(tagFilter = tag) }
            tagManager.updateLastUsage(tag)
        }
        val idsMatchingTag: List<Long> = tagManager.getMediaMatchingTag(tag, _state.value.mediaType)
        val tagOnlySearch = idsMatchingTag.isNotEmpty() && actualQuery.isBlank()

        if(tagOnlySearch){
            _state.update { currentState -> currentState.copy(tagOnlySearch = true) }
            return idsMatchingTag
        }
        if(actualQuery.isBlank()){
            return emptyList()
        }

        if(!textEmbedder.isInitialized())textEmbedder.initialize()

        val embedding = textEmbedder.embed(actualQuery)
        val queryEmbed = embedding.toQInt8Embed()
        val filterIds = idsMatchingTag.toSet()
        val queryResult = store.query(queryEmbed, Int.MAX_VALUE, TEXT_QUERY_THRESHOLD, filterIds,  startDate = startDate, endDate = endDate, includeSims = true)
        val clusterResult = clusterEmbedStore.query(queryEmbed, Int.MAX_VALUE, TEXT_QUERY_THRESHOLD, includeSims = true)
        val itemToSimMap = queryResultToMap(queryResult)
        val clusterToSimMap = queryResultToMap(clusterResult)
        val reranked = rerankItems(itemToSimMap, clusterToSimMap, clusterCrossRefRepository.getClusterToMediaIdsMap(), strictness)

        // prevent keeping both models open
        if(shouldShutdownModel(_state.value.imageEmbedderLastUsage)) imageEmbedder.closeSession()
        _state.update{it.copy(textEmbedderLastUsage = System.currentTimeMillis())}

        return reranked
    }

    private suspend fun imageSearch(store: FileEmbeddingStore, strictness: Float, startDate: Long? = null, endDate: Long? = null): List<Long> {
        val queryImage = _state.value.queryImage?: return  emptyList()

        if(!imageEmbedder.isInitialized()) imageEmbedder.initialize()

        val bitmap = getBitmapFromUri(getApplication(), queryImage, IMAGE_SIZE_X)
        val embedding = imageEmbedder.embed(bitmap)
        val queryEmbed= embedding.toQInt8Embed()
        val queryResult = store.query(queryEmbed, Int.MAX_VALUE, IMAGE_QUERY_THRESHOLD, startDate = startDate, endDate = endDate, includeSims = true)
        val clusterResult = clusterEmbedStore.query(queryEmbed, Int.MAX_VALUE, IMAGE_QUERY_THRESHOLD, includeSims = true)
        val itemToSimMap = queryResultToMap(queryResult)
        val clusterToSimMap = queryResultToMap(clusterResult)
        val reranked = rerankItems(itemToSimMap, clusterToSimMap, clusterCrossRefRepository.getClusterToMediaIdsMap(), strictness)

        // prevent keeping both models open
        if(shouldShutdownModel(_state.value.textEmbedderLastUsage)) textEmbedder.closeSession()
        _state.update { it.copy(imageEmbedderLastUsage = System.currentTimeMillis()) }

        return reranked
    }

    private suspend fun handleSearchResult(queryResults: List<Long>, store: FileEmbeddingStore, dedupeEnabled: Boolean = false) {
        val finalResults =  if (dedupeEnabled) dedupe(store, queryResults, DEDUPE_THRESHOLD) else queryResults
        cachedIds.addAll(finalResults)
        val totalCount = finalResults.size
        val initialBatch = finalResults.take(RESULTS_BATCH_SIZE) // initial results the rest loaded dynamically
        val (validIds, idsToPurge) = MediaStoreHelper.filterAccessibleMedia(getApplication(), initialBatch, _state.value.mediaType)
        val filteredSearchResults = validIds.map { toMediaItem(it, _state.value.mediaType) }

        _state.emit( _state.value.copy(totalResults = totalCount - idsToPurge.size, searchResults = filteredSearchResults))

        if (filteredSearchResults.isEmpty()) {
            _state.emit(_state.value.copy(error = getApplication<Application>().getString(R.string.search_error_no_results)))
        }

        if(idsToPurge.isNotEmpty()){
            cachedIds.removeAll(idsToPurge) // PREVENTS duplicates
            purgeStaleItems(store, idsToPurge)
        }
    }

    fun externalSearch(intentSearchQuery: SearchQuery?, similarityThreshold: Float, imageSimilarityThreshold: Float, dedupeEnabled: Boolean){
        if(intentSearchQuery == null || hasHandledExternalSearch) return

        when(intentSearchQuery) {
            is SearchQuery.ImageQuery -> {
                setMediaType(intentSearchQuery.mediaType)
                setQueryImage(intentSearchQuery.uri)
                search(imageSimilarityThreshold, dedupeEnabled)
                hasHandledExternalSearch = true
            }

            is SearchQuery.TextQuery -> {
                setMediaType(intentSearchQuery.mediaType)
                searchFieldState.edit { replace(0, searchFieldState.text.length, intentSearchQuery.text) }
                search( similarityThreshold, dedupeEnabled)
                hasHandledExternalSearch = true
            }
        }
    }

    fun onLoadMore() {
        if (isLoadingMoreSearchResults.getAndSet(true)) return
        val store = getStore()
        val currentItemsCount = _state.value.searchResults.size
        if (currentItemsCount >= _state.value.totalResults) return

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val batch = getPaginatedResult(currentItemsCount, RESULTS_BATCH_SIZE, cachedIds)
                val (filteredResults, idsToPurge) = MediaStoreHelper.filterAccessibleMedia(getApplication(), batch, _state.value.mediaType)

                if (filteredResults.isNotEmpty()) {
                    val filteredSearchResults = _state.value.searchResults + filteredResults.map { toMediaItem(it, _state.value.mediaType) }
                    _state.emit(_state.value.copy(searchResults = filteredSearchResults))
                }

                if (idsToPurge.isNotEmpty()) {
                    cachedIds.removeAll(idsToPurge) // PREVENTS duplicates
                    purgeStaleItems(store, idsToPurge)
                }
            }finally {
                isLoadingMoreSearchResults.set(false)
            }
        }
    }

    private fun purgeStaleItems(store: FileEmbeddingStore, idsToPurge: List<Long>){
        viewModelScope.launch(Dispatchers.IO) {
            removeStaleMedia(idsToPurge, _state.value.mediaType, store, mediaMetadataRepository)
        }
    }

    private fun viewResult(context: Context, item: MediaItem, autoOpenInGallery: Boolean? = null){
        if(!canOpenUri(context, item.uri)){
            _state.update { currentState -> currentState.copy(searchResults = currentState.searchResults - item) }
            return
        }

        if(autoOpenInGallery == true) {
            when(_state.value.mediaType){
                MediaType.IMAGE -> openImageInGallery(context, item.uri)
                MediaType.VIDEO -> openVideoInGallery(context, item.uri)
            }
        }else{
            _state.value = _state.value.copy(resultToView = item)
        }
    }

    private fun clearResultView() = _state.update {it.copy(resultToView = null)}

    private fun setQueryImage(uri: Uri?) = _state.update{it.copy(queryImage = uri)}

    private fun setStartDateFilter(date: Long?) = _state.update {it.copy(startDateFilter = date)}

    private fun setEndDateFilter(date: Long?) = _state.update {it.copy(endDateFilter = date)}

    private fun clearDateFilters() = _state.update {it.copy(endDateFilter = null, startDateFilter = null)}

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

    private fun shouldShutdownModel(lastUsage: Long?) = lastUsage != null && System.currentTimeMillis() - lastUsage >= MODEL_SHUTDOWN_DURATION_THRESHOLD
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
    private fun getStore() = if(_state.value.mediaType == MediaType.VIDEO) videoEmbedStore else imageEmbedStore

    private fun toggleSelectedResult(item: MediaItem){
        _state.update { it.copy(selection = SelectionUtils.toggleSelectedItem(it.selection, item, it.totalResults)) }
    }

    private fun setSelectAll(selectAll: Boolean) {
        _state.update { it.copy(selection = SelectionUtils.setSelectAll(it.selection, selectAll, it.totalResults))}
    }

    private suspend fun getSelectedResults(): Set<MediaItem> = SelectionUtils.getSelectedItems(_state.value.selection){getAllResults()}

    private suspend fun getAllResults(): MutableSet<MediaItem> {
        return withContext(Dispatchers.IO) {
            val mediaMetadataList = mediaMetadataRepository.getByIds(cachedIds, _state.value.mediaType)
            mediaMetadataList.map { it.toItem() }.toMutableSet()
        }
    }

    private fun queryResultToMap(result: QueryResult): Map<Long, Float> = result.sims?.let(result.ids::zip)?.toMap() ?: emptyMap()

    override fun onCleared() {
        textEmbedder.closeSession()
        imageEmbedder.closeSession()
        super.onCleared()
    }
}
