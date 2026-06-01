package com.fpf.smartscan.ui.screens.collections

import android.app.Application
import android.database.sqlite.SQLiteConstraintException
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fpf.smartscan.cluster.ClusterManager
import com.fpf.smartscan.data.tags.TagCrossRefRepository
import com.fpf.smartscan.data.tags.TagRepository
import com.fpf.smartscan.media.MediaCollection
import com.fpf.smartscan.data.clusters.ClusterCrossRefRepository
import com.fpf.smartscan.data.clusters.ClusterMetadataRepository
import com.fpf.smartscan.data.metadata.MediaMetadataRepository
import com.fpf.smartscan.data.tags.Tag
import com.fpf.smartscan.data.tags.TagCrossRef
import com.fpf.smartscan.events.CollectionEvent
import com.fpf.smartscan.events.CollectionEventType
import com.fpf.smartscan.media.CollectionType
import com.fpf.smartscan.tag.TagManager
import com.fpf.smartscan.ui.action.CollectionAction
import com.fpf.smartscan.ui.state.CollectionsState
import com.fpf.smartscan.ui.utils.SelectionUtils
import com.fpf.smartscansdk.core.embeddings.FileEmbeddingStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.collections.forEach

class CollectionsViewModel( 
    application: Application,
    private val tagRepository: TagRepository,
    private val tagCrossRefRepository: TagCrossRefRepository,
    private val clusterMetadataRepository: ClusterMetadataRepository,
    private val clusterCrossRefRepository: ClusterCrossRefRepository,
    private val mediaMetadataRepository: MediaMetadataRepository,
    private val imageStore: FileEmbeddingStore,
    private val videoStore: FileEmbeddingStore,
    clusterStore: FileEmbeddingStore,
    ) : AndroidViewModel(application) {
    companion object {
        private const val TAG = "CollectionsViewModel"
        const val TOP_N = 6
    }

    val tagManager = TagManager(
        tagRepository=tagRepository,
        tagCrossRefRepository=tagCrossRefRepository,
        mediaMetadataRepository = mediaMetadataRepository,
    )
    val clusterManager = ClusterManager(
        clusterStore = clusterStore,
        clusterCrossRefRepository = clusterCrossRefRepository,
        clusterMetadataRepository = clusterMetadataRepository,
        mediaMetadataRepository = mediaMetadataRepository,
    )

    private val _state = MutableStateFlow(CollectionsState())
    val state: StateFlow<CollectionsState> = _state

    val clusterCollections: StateFlow<List<MediaCollection>> = combine(
        clusterCrossRefRepository.getClustersWithCount(),
        _state.map {  it.showAllCollections to  it.collectionType }.distinctUntilChanged()
    ) { clusters, ( showAllCollections, collectionType) ->
        if(collectionType == CollectionType.CLUSTER){
            _state.update { it.copy(totalCollections = clusters.size) }
        }
        val filteredClusters = if (showAllCollections) clusters else clusters.take(TOP_N)

        clusterManager.toCollections(filteredClusters)
    }.flowOn(Dispatchers.IO)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Lazily,
            initialValue = emptyList()
        )

    val tagCollections: StateFlow<List<MediaCollection>> = combine(
        tagCrossRefRepository.getTagsWithCounts(),
        _state.map {  it.showAllCollections to  it.collectionType }.distinctUntilChanged()
    ) { tagsWithCount, ( showAllCollections, collectionType) ->
        if(collectionType == CollectionType.TAG){
            _state.update { it.copy(totalCollections = tagsWithCount.size) }
        }
        val tags = if (showAllCollections) tagsWithCount else tagsWithCount.take(TOP_N)
        tagManager.toCollections(tags)
    }.flowOn(Dispatchers.IO)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList()
        )

    private val _event = MutableSharedFlow<CollectionEvent>()
    val event = _event.asSharedFlow()

    fun onAction(action: CollectionAction){
        when(action){
            is CollectionAction.MergeCollections -> mergeCollections(action.primaryCollectionName, action.isNewMergedLabel)
            is CollectionAction.RenameCollection -> renameCollection(action.newName)
            is CollectionAction.CreateNewTagAndTagClusters -> createNewTagAndTagClusters(action.newName)
            is CollectionAction.ToggleSelectedCollection -> toggleSelectedCollection(action.collection)
            is CollectionAction.SetCollectionToView -> setCollectionToView(action.collection)
            is CollectionAction.SetCollectionType -> setCollectionType(action.type)
            is CollectionAction.DeleteCollections -> deleteCollections()
            is CollectionAction.ToggleViewAllCollections -> toggleViewAllCollections()
            is CollectionAction.SetSelectAll -> setSelectAll(action.selectAll)
            is CollectionAction.ToggleSelectionMode -> toggleSelectionMode()
            is CollectionAction.ClearSelection -> clearSelection()
            is CollectionAction.ResetSelection -> resetSelection()
        }
    }

    private fun clearSelection() = _state.update{it.copy(selection = SelectionUtils.clearSelection(it.selection))}
    private fun resetSelection() = _state.update{it.copy(selection = SelectionUtils.resetSelection(it.selection))}
    private fun toggleSelectionMode() = _state.update { it.copy(selection = SelectionUtils.toggleSelectionMode(it.selection)) }

    private fun renameCollection(newName: String){
        viewModelScope.launch(Dispatchers.IO) {
            try{
                val collection = getSelectedCollections().first()
                when (collection.type) {
                    CollectionType.CLUSTER -> clusterManager.updateLabel(collection.id, newName)
                    CollectionType.TAG -> tagManager.renameTag(collection.name, newName)
                }
                resetSelection()
                _event.emit(CollectionEvent(CollectionEventType.RENAME, success = true))
            } catch (_: SQLiteConstraintException){
                _event.emit(CollectionEvent(CollectionEventType.RENAME, success = false, message = "Collection already exists"))
            }
            catch (e: Exception){
                Log.e(TAG, "Error renaming collection: ${e.message}")
                _event.emit(CollectionEvent(CollectionEventType.RENAME, success = false, message = "Error renaming collection"))

            }
        }
    }

    private fun deleteCollections(){
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val selectedCollections = getSelectedCollections()
                tagRepository.deleteTagsByName(selectedCollections.map{it.name})
                resetSelection()
                _event.emit(CollectionEvent(CollectionEventType.DELETE, success = true))
            }catch (e: Exception){
                Log.e(TAG, "Error deleting collections: ${e.message}")
                _event.emit(CollectionEvent(CollectionEventType.DELETE, success = false, message = "Error deleting collections"))
            }
        }
    }

    private fun mergeCollections(primaryCollectionName: String, isNewMergedLabel: Boolean){
        _state.update { it.copy(loading = true) }

        viewModelScope.launch (Dispatchers.IO) {
            try {
                val selectedCollections = getSelectedCollections()
                var primaryCollection = selectedCollections.firstOrNull{it.name == primaryCollectionName}

                if(isNewMergedLabel) {
                    primaryCollection = selectedCollections.firstOrNull()
                    primaryCollection?.let { collection ->
                        when (collection.type) {
                            CollectionType.CLUSTER -> clusterManager.updateLabel(collection.id, primaryCollectionName)
                            CollectionType.TAG -> tagManager.renameTag(collection.name, primaryCollectionName)
                        }
                    }
                }

                primaryCollection?.let { collection ->
                    val otherCollections = selectedCollections.filter { selectedCollection -> selectedCollection.id != collection.id }
                    when (collection.type) {
                        CollectionType.CLUSTER -> clusterManager.mergeClusters(collection.id, otherCollections.map { it.id }, imageStore, videoStore)
                        CollectionType.TAG -> tagManager.mergeTags(primaryCollectionName, otherCollections.map { it.name })
                    }
                }
                resetSelection()
                _event.emit(CollectionEvent(CollectionEventType.MERGE, success = true))
            }
            catch (_: SQLiteConstraintException){
                _event.emit(CollectionEvent(CollectionEventType.MERGE, success = false, message = "Collection already exists"))
            }
            catch (e: Exception){
                Log.e(TAG, "Error merging collections: ${e.message}")
                _event.emit(CollectionEvent(CollectionEventType.MERGE, success = false, message = "Error merging collections"))
            }finally {
                _state.update { it.copy(loading = false) }
            }
        }
    }

    private suspend fun tagCluster(clusterId: Long, tagId: Long){
        val clusterCrossRefs = mediaMetadataRepository.getByCluster(clusterId)
        tagCrossRefRepository.insertTagCrossRefs(clusterCrossRefs.map{ TagCrossRef(it.id, tagId)})
    }

    private fun createNewTagAndTagClusters(newTag: String){
        _state.update { it.copy(loading = true) }

        viewModelScope.launch (Dispatchers.IO) {
            try {
                val selectedCollections = getSelectedCollections()
                val tagId = tagRepository.insertTags(listOf(Tag(name = newTag))).firstOrNull()?: return@launch
                selectedCollections.forEach { tagCluster(it.id, tagId) }
                resetSelection()
                _event.emit(CollectionEvent(CollectionEventType.COPY, success = true))
            }catch (_: SQLiteConstraintException){
                _event.emit(CollectionEvent(CollectionEventType.COPY, success = false, message = "Collection already exists"))
            }catch (e: Exception){
                Log.e(TAG, "Error copying collections: ${e.message}")
                _event.emit(CollectionEvent(CollectionEventType.COPY, success = false, message = "Error copying collection(s)"))
            }
            finally {
                _state.update { it.copy(loading = false) }
            }
        }
    }

    private fun setCollectionType(type: CollectionType) {
        resetSelection()
        _state.update { it.copy(collectionType = type) }
    }

    private fun toggleViewAllCollections() = _state.update{ it.copy(showAllCollections = !it.showAllCollections)}
    private fun setCollectionToView(collection: MediaCollection?) = _state.update { it.copy(collectToView = collection) }

    private fun toggleSelectedCollection(item: MediaCollection){
        _state.update { it.copy(selection = SelectionUtils.toggleSelectedItem(it.selection, item, it.totalCollections)) }
    }

    private fun setSelectAll(selectAll: Boolean) {
        _state.update { it.copy(selection = SelectionUtils.setSelectAll(it.selection, selectAll, it.totalCollections))}
    }
    private suspend fun getSelectedCollections(): Set<MediaCollection> = SelectionUtils.getSelectedItems(_state.value.selection){getAllCollections()}

    private suspend fun getAllCollections(): MutableSet<MediaCollection>{
        val currentState = state.value
        return when (currentState.collectionType ){
            CollectionType.CLUSTER -> {
                if(currentState.showAllCollections) {
                    clusterCollections.value
                } else {
                    clusterManager.toCollections(clusterCrossRefRepository.getClustersWithCount().first() )
                }
            }
            CollectionType.TAG -> {
                if(currentState.showAllCollections) {
                    tagCollections.value
                } else {
                    tagManager.toCollections(tagCrossRefRepository.getTagsWithCounts().first())
                }
            }
        }.toMutableSet()
    }
}
