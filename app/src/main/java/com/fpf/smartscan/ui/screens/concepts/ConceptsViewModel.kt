package com.fpf.smartscan.ui.screens.concepts

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fpf.smartscan.cluster.ClusterManager
import com.fpf.smartscan.concepts.Concept
import com.fpf.smartscan.data.tags.TagCrossRefRepository
import com.fpf.smartscan.data.tags.TagRepository
import com.fpf.smartscan.data.clusters.ClusterCrossRefRepository
import com.fpf.smartscan.data.clusters.ClusterMetadataRepository
import com.fpf.smartscan.data.metadata.MediaMetadataRepository
import com.fpf.smartscan.events.CollectionEvent
import com.fpf.smartscan.media.CollectionType
import com.fpf.smartscan.media.MediaCollection
import com.fpf.smartscan.tag.TagManager
import com.fpf.smartscan.ui.action.ConceptAction
import com.fpf.smartscan.ui.state.ConceptsState
import com.fpf.smartscan.ui.state.common.SelectionState
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
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

class ConceptsViewModel(
    application: Application,
    private val tagRepository: TagRepository,
    private val tagCrossRefRepository: TagCrossRefRepository,
    private val clusterMetadataRepository: ClusterMetadataRepository,
    private val clusterCrossRefRepository: ClusterCrossRefRepository,
    private val mediaMetadataRepository: MediaMetadataRepository,
    imageStore: FileEmbeddingStore,
    videoStore: FileEmbeddingStore,
    clusterStore: FileEmbeddingStore,
) : AndroidViewModel(application) {
    companion object {
        private const val TAG = "ConceptsViewModel"
    }

    // TODO: add real concepts from db
    val concepts = listOf<Concept>(
        Concept(
            id=0,
            description = "Turn years of interviews into an actionable knowledge base.",
            size = 2
        ),
        Concept(
            id=1,
            description = "Infrastructure, scale, or domain expertise that is hard to replicate",
            size = 9
        )
    )

    val tagManager = TagManager(
        tagRepository = tagRepository,
        tagCrossRefRepository = tagCrossRefRepository,
        mediaMetadataRepository = mediaMetadataRepository,
    )
    val clusterManager = ClusterManager(
        clusterEmbedStore = clusterStore,
        imageEmbedStore = imageStore,
        videoEmbedStore = videoStore,
        clusterCrossRefRepository = clusterCrossRefRepository,
        clusterMetadataRepository = clusterMetadataRepository,
        mediaMetadataRepository = mediaMetadataRepository,
    )

    private val _state = MutableStateFlow(ConceptsState())
    val state: StateFlow<ConceptsState> = _state



    val clusterCollections: StateFlow<List<MediaCollection>> = combine(
        clusterCrossRefRepository.getClustersWithCount(),
        _state.map {  it.selectedCollectionType }.distinctUntilChanged()
    ) { clusters, collectionType ->
        if(collectionType == CollectionType.CLUSTER){
            _state.update { it.copy(totalCollections = clusters.size) }
        }
        clusterManager.toCollections(clusters)
    }.flowOn(Dispatchers.IO)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Lazily,
            initialValue = emptyList()
        )

    val tagCollections: StateFlow<List<MediaCollection>> = combine(
        tagCrossRefRepository.getTagsWithCounts(),
        _state.map {  it.selectedCollectionType }.distinctUntilChanged()
    ) { tagsWithCount, collectionType ->
        if(collectionType == CollectionType.TAG){
            _state.update { it.copy(totalCollections = tagsWithCount.size) }
        }
        tagManager.toCollections(tagsWithCount)
    }.flowOn(Dispatchers.IO)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList()
        )

    private val _event = MutableSharedFlow<CollectionEvent>()
    val event = _event.asSharedFlow()

    fun onAction(action: ConceptAction) {
        when (action) {
            is ConceptAction.ToggleSelectionMode -> toggleSelectionMode()
            is ConceptAction.ClearSelection -> clearSelection()
            is ConceptAction.ResetSelection -> resetSelection()
            is ConceptAction.SetSelectAll -> setSelectAll(action.selectAll)
            is ConceptAction.DeleteConcept -> TODO()
            is ConceptAction.EditConcept -> TODO()
            is ConceptAction.SetConceptToView -> setConceptToView(action.concept)
            is ConceptAction.ToggleSelectedConcept -> toggleSelectedConcept(action.concept)
            is ConceptAction.ToggleViewAllConcepts -> toggleViewAllConcepts()
            is ConceptAction.SetAllowedCollections -> setAllowedCollections()
            is ConceptAction.SetCollectionType -> setCollectionType(action.collectionType)
            is ConceptAction.ToggleSelectedCollection -> toggleSelectedCollection(action.collection)
        }
    }

    private fun clearSelection() = _state.update { it.copy(selection = SelectionUtils.clearSelection(it.selection)) }

    private fun resetSelection() = _state.update { it.copy(selection = SelectionUtils.resetSelection(it.selection)) }

    private fun toggleSelectionMode() = _state.update { it.copy(selection = SelectionUtils.toggleSelectionMode(it.selection)) }

    private fun toggleViewAllConcepts() = _state.update { it.copy(showAllConcepts = !it.showAllConcepts) }

    private fun setConceptToView(concept: Concept?) = _state.update { it.copy(conceptToView = concept) }

    private fun toggleSelectedConcept(item: Concept) { _state.update {
            it.copy(
                selection = SelectionUtils.toggleSelectedItem(
                    it.selection,
                    item,
                    it.totalConcepts
                )
            )
        }
    }

    private fun toggleSelectedCollection(item: MediaCollection) { _state.update {
        it.copy(
            collectionsSelection = SelectionUtils.toggleSelectedItem(
                it.collectionsSelection,
                item,
                it.totalCollections
            )
        )
    }
    }

    private fun setCollectionType(type: CollectionType?) = _state.update { it.copy(selectedCollectionType = type) }

    private fun setSelectAll(selectAll: Boolean) {
        _state.update {
            it.copy(
                selection = SelectionUtils.setSelectAll(
                    it.selection,
                    selectAll,
                    it.totalConcepts
                )
            )
        }
    }

    private fun setAllowedCollections(){
        val selectedCollections = _state.value.collectionsSelection.selectedItems
        Log.d(TAG, "Allowed collections: \n${selectedCollections.toString()}")
        setCollectionType(null)
    }


    private suspend fun getSelectedConcepts(): Set<Concept> = SelectionUtils.getSelectedItems(_state.value.selection) { getAllConcepts() }

    private suspend fun getAllConcepts(): MutableSet<Concept> {
        val currentState = state.value
        return mutableSetOf()
    }
}
