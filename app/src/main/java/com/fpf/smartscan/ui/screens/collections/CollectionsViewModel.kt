package com.fpf.smartscan.ui.screens.collections

import android.app.Application
import android.database.sqlite.SQLiteConstraintException
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fpf.smartscan.core.cluster.ClusterManager
import com.fpf.smartscan.core.media.MediaCollection
import com.fpf.smartscan.events.CollectionEvent
import com.fpf.smartscan.events.CollectionEventType
import com.fpf.smartscan.core.media.CollectionType
import com.fpf.smartscan.core.tag.TagManager
import com.fpf.smartscan.ui.state.CollectionsState
import com.fpf.smartscan.ui.utils.SelectionUtils
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

class CollectionsViewModel(
    application: Application,
    private val tagManager: TagManager,
    private val clusterManager: ClusterManager,
    ) : AndroidViewModel(application) {
    companion object {
        private const val TAG = "CollectionsViewModel"
        const val TOP_N = 6
    }

    private val _state = MutableStateFlow(CollectionsState())
    val state: StateFlow<CollectionsState> = _state

    val clusterCollections: StateFlow<List<MediaCollection>> = combine(
        clusterManager.allCollectionsFlow,
        _state.map {  it.showAllCollections to  it.collectionType }.distinctUntilChanged()
    ) { collections, ( showAllCollections, collectionType) ->
        if(collectionType == CollectionType.CLUSTER){
            _state.update { it.copy(totalCollections = collections.size) }
        }
        val filterCollections = if (showAllCollections) collections else collections.take(TOP_N)
        filterCollections
    }.flowOn(Dispatchers.IO)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Lazily,
            initialValue = emptyList()
        )

    val tagCollections: StateFlow<List<MediaCollection>> = combine(
        tagManager.allCollectionsFlow,
        _state.map {  it.showAllCollections to  it.collectionType }.distinctUntilChanged()
    ) { collections, ( showAllCollections, collectionType) ->
        if(collectionType == CollectionType.TAG){
            _state.update { it.copy(totalCollections = collections.size) }
        }
        val filteredCollections = if (showAllCollections) collections else collections.take(TOP_N)
        filteredCollections
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
                tagManager.deleteTags(selectedCollections.map{it.id})
                resetSelection()
                val message = if(selectedCollections.size == 1 ) "Deleted ${selectedCollections.size} collection" else "Deleted ${selectedCollections.size} collections"
                _event.emit(CollectionEvent(CollectionEventType.DELETE, success = true, message = message))
            }catch (e: Exception){
                val message = "Error deleting collections"
                Log.e(TAG, "$message: ${e.message}")
                _event.emit(CollectionEvent(CollectionEventType.DELETE, success = false, message = message))
            }
        }
    }

    private fun mergeCollections(primaryCollectionName: String, isNewMergedLabel: Boolean){
        _state.update { it.copy(loading = true) }

        viewModelScope.launch (Dispatchers.IO) {
            try {
                val selectedCollections = getSelectedCollections()
                if(selectedCollections.size < 2 ) return@launch
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

                val newMergedCollection = primaryCollection?: error("No primary collection selected")
                val otherCollections = selectedCollections.filter { selectedCollection -> selectedCollection.id != newMergedCollection.id }
                when (newMergedCollection.type) {
                    CollectionType.CLUSTER -> clusterManager.mergeClusters(newMergedCollection.id, otherCollections.map { it.id })
                    CollectionType.TAG -> tagManager.mergeTags(newMergedCollection.id, otherCollections.map { it.id })
                }

                resetSelection()
                _event.emit(CollectionEvent(CollectionEventType.MERGE, success = true, "Merged ${selectedCollections.size} collections"))
            }
            catch (_: SQLiteConstraintException){
                _event.emit(CollectionEvent(CollectionEventType.MERGE, success = false, message = "Collection already exists"))
            }
            catch (e: Exception){
                val message = "Error merging collections"
                Log.e(TAG, "$message: ${e.message}")
                _event.emit(CollectionEvent(CollectionEventType.MERGE, success = false, message = message))
            }finally {
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
                    clusterManager.allCollectionsFlow.first()
                }
            }
            CollectionType.TAG -> {
                if(currentState.showAllCollections) {
                    tagCollections.value
                } else {
                    tagManager.allCollectionsFlow.first()
                }
            }
        }.toMutableSet()
    }
}
