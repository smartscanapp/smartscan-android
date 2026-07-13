package com.fpf.smartscan.ui.screens.collections

import android.app.Application
import android.content.ClipData
import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import android.util.Log
import androidx.compose.ui.platform.Clipboard
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import coil3.compose.AsyncImagePainter
import com.fpf.smartscan.cluster.ClusterManager
import com.fpf.smartscan.data.clusters.ClusterCrossRefRepository
import com.fpf.smartscan.data.clusters.ClusterMetadataRepository
import com.fpf.smartscan.media.MediaCollection
import com.fpf.smartscan.data.tags.TagPagingSource
import com.fpf.smartscan.data.clusters.ClusterPagingSource
import com.fpf.smartscan.data.metadata.MediaMetadataRepository
import com.fpf.smartscan.data.tags.TagCrossRefRepository
import com.fpf.smartscan.data.tags.TagRepository
import com.fpf.smartscan.events.CollectionItemEvent
import com.fpf.smartscan.events.CollectionItemEventType
import com.fpf.smartscan.media.CollectionType
import com.fpf.smartscan.media.MediaItem
import com.fpf.smartscan.media.MediaType
import com.fpf.smartscan.media.openImageInGallery
import com.fpf.smartscan.media.openVideoInGallery
import com.fpf.smartscan.media.onMediaLoadingError
import com.fpf.smartscan.media.shareMediaMulti
import com.fpf.smartscan.media.toItem
import com.fpf.smartscan.tag.TagManager
import com.fpf.smartscan.ui.action.CollectionItemAction
import com.fpf.smartscan.ui.state.CollectionItemsState
import com.fpf.smartscan.ui.utils.SelectionUtils
import com.fpf.smartscansdk.core.embeddings.FileEmbeddingStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.collections.map

class CollectionItemsViewModel(
    application: Application,
    private val imageEmbedStore: FileEmbeddingStore,
    private val videoEmbedStore: FileEmbeddingStore,
    private val clusterEmbedStore: FileEmbeddingStore,
    private val tagRepository: TagRepository,
    private val tagCrossRefRepository: TagCrossRefRepository,
    private val mediaMetadataRepository: MediaMetadataRepository,
    private val clusterMetadataRepository: ClusterMetadataRepository,
    private val clusterCrossRefRepository: ClusterCrossRefRepository,
) : AndroidViewModel(application) {
    companion object {
        private const val TAG = "CollectionItemsViewModel"
    }

    val tagManager = TagManager(
        tagRepository=tagRepository,
        tagCrossRefRepository=tagCrossRefRepository,
        mediaMetadataRepository = mediaMetadataRepository,
    )

    val clusterManager = ClusterManager(
        clusterEmbedStore = clusterEmbedStore,
        imageEmbedStore = imageEmbedStore,
        videoEmbedStore = videoEmbedStore,
        clusterCrossRefRepository = clusterCrossRefRepository,
        clusterMetadataRepository = clusterMetadataRepository,
        mediaMetadataRepository = mediaMetadataRepository,
    )
    private val _state = MutableStateFlow(CollectionItemsState())
    val state: StateFlow<CollectionItemsState> = _state

    @OptIn(ExperimentalCoroutinesApi::class)
    val tagItems = _state
        .map { it.mediaType to it.collection }
        .distinctUntilChanged()
        .flatMapLatest { (mediaType, collection) ->

            if (collection?.id == null) {
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
                        TagPagingSource(
                            mediaType = mediaType,
                            tagId = collection.id,
                            mediaMetadataRepository = mediaMetadataRepository,
                        )
                    }
                ).flow
            }
        }
        .cachedIn(viewModelScope)

    @OptIn(ExperimentalCoroutinesApi::class)
    val clusterItems = _state
        .map { it.mediaType to it.collection }
        .distinctUntilChanged()
        .flatMapLatest { (mediaType, collection) ->
            if (collection?.id == null) {
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
                        ClusterPagingSource(
                            mediaType = mediaType,
                            clusterId = collection.id,
                            mediaMetadataRepository = mediaMetadataRepository,
                        )
                    }
                ).flow
            }
        }
        .cachedIn(viewModelScope)

    val tagCollections: StateFlow<List<MediaCollection>> = tagRepository.getCollections()
        .flowOn(Dispatchers.IO)
        .stateIn(
        scope = viewModelScope,
        started = SharingStarted.Lazily,
        initialValue = emptyList()
    )

    val clusterCollections: StateFlow<List<MediaCollection>> = clusterMetadataRepository.getCollections()
            .flowOn(Dispatchers.IO)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.Lazily,
                initialValue = emptyList()
            )

    private val _event = MutableSharedFlow<CollectionItemEvent>()
    val event = _event.asSharedFlow()

    fun onAction(action: CollectionItemAction){
        when(action){
            is CollectionItemAction.CopyMedia -> copyItem(action.clipboard, action.context)
            is CollectionItemAction.CreateNewCollectionAndMove -> createNewCollectionAndMove(action.newName)
            CollectionItemAction.RemoveMedia -> removeItems()
            is CollectionItemAction.SetMediaToView -> setMediaToView(action.context, action.item, autoOpenInGallery = action.autoOpenInGallery)
            is CollectionItemAction.ShareMedia -> shareItems(action.context)
            is CollectionItemAction.ToggleSelectedMedia -> toggleSelectedItem(action.item)
            is CollectionItemAction.SetCollectionToView -> setCollection(action.collection)
            is CollectionItemAction.MoveMedia -> moveItems(action.destinationCollection)
            is CollectionItemAction.Tag -> tagItems(action.tag)
            is CollectionItemAction.SetSelectAll -> setSelectAll(action.selectAll)
            is CollectionItemAction.ToggleSelectionMode -> toggleSelectionMode()
            is CollectionItemAction.ResetSelection -> resetSelection()
            is CollectionItemAction.ClearSelection -> clearSelection()
            is CollectionItemAction.SetMediaTypeFilter -> setMediaTypeFilter(action.mediaType)
            is CollectionItemAction.SaveUpdatedItem -> saveUpdatedItem(action.updatedItem)
            }
    }

    private fun clearSelection() = _state.update{it.copy(selection = SelectionUtils.clearSelection(it.selection))}
    private fun resetSelection() = _state.update{it.copy(selection = SelectionUtils.resetSelection(it.selection))}
    private fun toggleSelectionMode() = _state.update { it.copy(selection = SelectionUtils.toggleSelectionMode(it.selection)) }


    private fun tagItems(tag: String){
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val selectedItems = getSelectedItems()
                tagManager.tagItems(tag, selectedItems)
                val message = if(selectedItems.size == 1 ) "Tagged ${selectedItems.size} item" else "Tagged ${selectedItems.size} items"
                _event.emit(CollectionItemEvent(CollectionItemEventType.TAG, success = true, message = message))
            }catch (e: Exception){
                val message = "Error tagging items"
                Log.e(TAG, "$message: ${e.message}")
                _event.emit(CollectionItemEvent(CollectionItemEventType.TAG, success = false, message = message))
            }
        }
    }

    fun handleAutoCompletionCheck(query: CharSequence, substringEnd: Int, startWithHashtag: Boolean =  true): List<String>{
        return tagManager.checkAutoCompletion(query, substringEnd, tagCollections.value.map { it.name }, startWithHashtag)
    }

    private fun removeItems(){
        val currentCollection = _state.value.collection?: return
        if(currentCollection.type != CollectionType.TAG) return // Only allowed for tag collections

        viewModelScope.launch (Dispatchers.IO){
            try {
                val selectedItems = getSelectedItems()
                tagManager.removeItems(currentCollection.name, selectedItems)
                resetSelection()
                val message = if(selectedItems.size == 1 ) "Removed ${selectedItems.size} item" else "Removed ${selectedItems.size} items"
                _event.emit(CollectionItemEvent(CollectionItemEventType.REMOVE, success = true, message = message))
            }catch (e: Exception){
                val message = "Error removing items"
                Log.e(TAG, "$message: ${e.message}")
                _event.emit(CollectionItemEvent(CollectionItemEventType.REMOVE, success = false, message = message))
            }
        }
    }

    private fun moveItems(newCollection: MediaCollection){
        val currentCollection = _state.value.collection?: return
        _state.update { it.copy(loading = true) }

        viewModelScope.launch (Dispatchers.IO){
            try {
                val selectedItems = getSelectedItems()
                if (selectedItems.isEmpty()) return@launch
                when(newCollection.type) {
                    CollectionType.CLUSTER -> clusterManager.moveItems(selectedItems, newCollection.id, currentCollection.id)
                    CollectionType.TAG -> tagManager.moveItems(selectedItems, currentCollection.name, newCollection.name)
                }
                resetSelection()
                val message = if(selectedItems.size == 1 ) "Moved ${selectedItems.size} item" else "Moved ${selectedItems.size} items"
                _event.emit(CollectionItemEvent(CollectionItemEventType.MOVE, success = true, message = message))
            }catch (e: Exception){
                val message = "Error moving items"
                Log.e(TAG, "$message: ${e.message}")
                _event.emit(CollectionItemEvent(CollectionItemEventType.MOVE, success = false, message = message))
            }finally {
                _state.update { it.copy(loading = false) }
            }
        }
    }

    private fun copyItem(clipboard: Clipboard, context: Context){
        viewModelScope.launch {
            val itemToCopy = getSelectedItems().first().uri
            clipboard.nativeClipboard.setPrimaryClip(ClipData.newUri(context.contentResolver, "smartscan_media", itemToCopy))
            _event.emit(CollectionItemEvent(CollectionItemEventType.COPY, success = true))
            resetSelection()
        }
    }

    private fun shareItems(context: Context){
        viewModelScope.launch {
            val items = getSelectedItems()
            shareMediaMulti(context, items.map{it.uri})
            _event.emit(CollectionItemEvent(CollectionItemEventType.SHARE, success = true))
            resetSelection()
        }
    }

    private fun createNewCollectionAndMove( newCollectionName: String){
        val state = _state.value
        val currentCollection = state.collection?: return
        if(currentCollection.name == newCollectionName) return

        viewModelScope.launch (Dispatchers.IO) {
            try {
                val selectedItems = getSelectedItems()
                if (selectedItems.isEmpty()) return@launch
                when(currentCollection.type){
                    CollectionType.TAG -> tagManager.createNewTagAndMoveItems(selectedItems, currentCollection.name, newCollectionName)
                    CollectionType.CLUSTER -> clusterManager.createNewClusterAndMoveItems(selectedItems, newCollectionName, currentCollection.id)
                }
                resetSelection()
                val message = if(selectedItems.size == 1 ) "Moved ${selectedItems.size} item" else "Moved ${selectedItems.size} items"
                _event.emit(CollectionItemEvent(CollectionItemEventType.MOVE, success = true, message = message))
            }catch (_: SQLiteConstraintException){
                _event.emit(CollectionItemEvent(CollectionItemEventType.MOVE, success = false, message = "Collection already exists"))
            }catch (e: Exception){
                val message = "Error moving items"
                Log.e(TAG, "$message: ${e.message}")
                _event.emit(CollectionItemEvent(CollectionItemEventType.MOVE, success = false, message = message))            }
        }
    }


   private fun toggleSelectedItem(item: MediaItem){
       _state.update {
           val collection = it.collection ?: return
           it.copy(selection = SelectionUtils.toggleSelectedItem(it.selection, item, collection.size))
       }
   }

    private fun setSelectAll(selectAll: Boolean) {
        val currentState = _state.value
        val collection = currentState.collection?: return
        _state.update { it.copy(selection = SelectionUtils.setSelectAll(it.selection, selectAll, collection.size))}

    }

    private suspend fun getSelectedItems(): Set<MediaItem> = SelectionUtils.getSelectedItems(_state.value.selection){getAllItemInCollection()}

    private suspend fun getAllItemInCollection(): MutableSet<MediaItem> {
        val currentState = state.value
        val currentCollection = currentState.collection ?: return mutableSetOf()
        return when (currentCollection.type) {
            CollectionType.CLUSTER -> {
                val itemsMatchingCluster = mediaMetadataRepository.getByCluster(currentCollection.id)
                itemsMatchingCluster.map { it.toItem() }.toMutableSet()
            }

            CollectionType.TAG -> {
                val itemsMatchingTag = mediaMetadataRepository.getByTag(currentCollection.id)
                itemsMatchingTag.map { it.toItem() }.toMutableSet()
            }
        }
    }

    private fun setCollection(collection: MediaCollection?) = _state.update { it.copy(collection=collection) }

    private fun setMediaToView(context: Context, item: MediaItem?, autoOpenInGallery: Boolean? = null){
        if(autoOpenInGallery == true) {
            when(item?.type){
                MediaType.IMAGE -> openImageInGallery(context, item.uri)
                MediaType.VIDEO -> openVideoInGallery(context, item.uri)
                else -> {}
            }
        }else{
            _state.update { it.copy(mediaToView =item) }
        }
    }

    private fun setMediaTypeFilter(mediaType: MediaType?) = _state.update { it.copy(mediaType=mediaType) }

    private fun saveUpdatedItem(updatedItem: MediaItem){
        viewModelScope.launch(Dispatchers.IO) {
            val meta = mediaMetadataRepository.getByIds(listOf(updatedItem.id), updatedItem.type).firstOrNull()?: return@launch
            val updatedMeta = meta.copy(description = updatedItem.description)
            mediaMetadataRepository.update(listOf(updatedMeta))
            mediaMetadataRepository.addToRecentUpdates(updatedItem)
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
}
