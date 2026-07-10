package com.fpf.smartscan.ui.screens.concepts

import android.app.Application
import android.content.Context.MODE_PRIVATE
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fpf.smartscan.cluster.ClusterManager
import com.fpf.smartscan.concepts.Concept
import com.fpf.smartscan.concepts.ConceptManager
import com.fpf.smartscan.concepts.getAllowedClusters
import com.fpf.smartscan.concepts.getAllowedTags
import com.fpf.smartscan.concepts.setAllowedClusters
import com.fpf.smartscan.concepts.setAllowedTags
import com.fpf.smartscan.constants.PrefsKeys
import com.fpf.smartscan.constants.PrefsNames
import com.fpf.smartscan.data.tags.TagCrossRefRepository
import com.fpf.smartscan.data.tags.TagRepository
import com.fpf.smartscan.data.clusters.ClusterCrossRefRepository
import com.fpf.smartscan.data.clusters.ClusterMetadataRepository
import com.fpf.smartscan.data.concepts.ConceptCrossRefRepository
import com.fpf.smartscan.data.concepts.ConceptRepository
import com.fpf.smartscan.data.metadata.MediaMetadataRepository
import com.fpf.smartscan.events.CollectionEvent
import com.fpf.smartscan.media.CollectionType
import com.fpf.smartscan.media.MediaCollection
import com.fpf.smartscan.tag.TagManager
import com.fpf.smartscan.ui.action.ConceptAction
import com.fpf.smartscan.ui.state.ConceptsState
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.onEach
class ConceptsViewModel(
    application: Application,
    private val tagRepository: TagRepository,
    private val clusterMetadataRepository: ClusterMetadataRepository,
    private val conceptRepository: ConceptRepository,
    private val conceptCrossRefRepository: ConceptCrossRefRepository,
    private val conceptEmbedStore: FileEmbeddingStore,
    private val imageConceptEmbedStore: FileEmbeddingStore,
) : AndroidViewModel(application) {
    companion object {
        private const val TAG = "ConceptsViewModel"
        private const val SIMILARITY_THRESHOLD = 0.3f
    }

    private val sharedPrefs by lazy { application.getSharedPreferences(PrefsNames.APP_PREFS, MODE_PRIVATE)    }

    val conceptManager = ConceptManager(
        conceptRepository=conceptRepository,
        conceptCrossRefRepository=conceptCrossRefRepository,
        conceptEmbedStore=conceptEmbedStore,
        imageConceptEmbedStore=imageConceptEmbedStore
    )

    private val _state = MutableStateFlow(ConceptsState())
    val state: StateFlow<ConceptsState> = _state

    val concepts: StateFlow<List<Concept>> = conceptRepository.getConceptsFlow()
            .onEach { concepts -> _state.update { it.copy(totalConcepts = concepts.size) } }
            .flowOn(Dispatchers.IO)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.Lazily,
                initialValue = emptyList()
            )

    val clusterCollections: StateFlow<List<MediaCollection>> = combine(
        clusterMetadataRepository.getCollections(),
        _state.map {  it.selectedCollectionType }.distinctUntilChanged()
    ) { collections, collectionType ->
        if(collectionType == CollectionType.CLUSTER){
            _state.update { it.copy(totalCollections = collections.size) }
        }
        collections
    }.flowOn(Dispatchers.IO)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Lazily,
            initialValue = emptyList()
        )

    val tagCollections: StateFlow<List<MediaCollection>> = combine(
        tagRepository.getCollections(),
        _state.map {  it.selectedCollectionType }.distinctUntilChanged()
    ) { collections, collectionType ->
        if(collectionType == CollectionType.TAG){
            _state.update { it.copy(totalCollections = collections.size) }
        }
        collections
    }.flowOn(Dispatchers.IO)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList()
        )

    private val _event = MutableSharedFlow<CollectionEvent>()
    val event = _event.asSharedFlow()

    init {
        load()
    }

    fun onAction(action: ConceptAction) {
        when (action) {
            is ConceptAction.ToggleSelectionMode -> toggleSelectionMode()
            is ConceptAction.ClearSelection -> clearSelection()
            is ConceptAction.ResetSelection -> resetSelection()
            is ConceptAction.SetSelectAll -> setSelectAll(action.selectAll)
            is ConceptAction.DeleteConcepts -> deleteConcepts()
            is ConceptAction.UpdateConcept -> updateConcept(action.concept, action.newDescription)
            is ConceptAction.AddConcept -> addConcept(action.description)
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

    private fun addConcept(description: String){
        viewModelScope.launch(Dispatchers.IO) {
            val concept = conceptManager.createConcept(description)
            conceptManager.findAndUpdateMediaMatchingConcept(concept, SIMILARITY_THRESHOLD)
        }
    }

    private fun updateConcept(concept: Concept, newDescription: String){
        viewModelScope.launch(Dispatchers.IO) {
            conceptManager.updateConcept(concept, newDescription)
        }
    }

    private fun deleteConcepts(){
        viewModelScope.launch(Dispatchers.IO) {
            val concepts = _state.value.selection.selectedItems
            conceptManager.deleteConcepts(concepts.toList())
            resetSelection()
        }
    }

    private fun load(){
        viewModelScope.launch(Dispatchers.IO) {
            val allowedCollections = mutableListOf<MediaCollection>()
            allowedCollections.addAll(getAllowedClusterCollections())
            allowedCollections.addAll(getAllowedTagCollections())
            val updatedCollectionState = _state.value.collectionsSelection.copy(selectedItems = allowedCollections.toSet())
            _state.update { it.copy( collectionsSelection = updatedCollectionState)}
        }
    }

    // TODO: move to concept manager
    private suspend fun getAllowedTagCollections(): List<MediaCollection>{
        val tagIds = getAllowedTags(sharedPrefs)
        return tagRepository.getCollections(tagIds.toList())
    }

    private suspend fun getAllowedClusterCollections(): List<MediaCollection>{
        val clusterIds = getAllowedClusters(sharedPrefs)
        return clusterMetadataRepository.getCollections(clusterIds.toList())
    }

    private fun toggleSelectedCollection(item: MediaCollection) { _state.update {
        it.copy(
            collectionsSelection = SelectionUtils.toggleSelectedItem(
                it.collectionsSelection,
                item,
                it.totalCollections
            )
        ) }
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
        val currentState = _state.value
        val selectedCollections = currentState.collectionsSelection.selectedItems
        when(currentState.selectedCollectionType){
            CollectionType.CLUSTER -> setAllowedClusters(sharedPrefs, selectedCollections.map{it.id}.toSet())
            CollectionType.TAG -> setAllowedTags(sharedPrefs, selectedCollections.map{it.id}.toSet())
            else -> {}
        }
        setCollectionType(null)
    }

    private suspend fun getSelectedConcepts(): Set<Concept> = SelectionUtils.getSelectedItems(_state.value.selection) { getAllConcepts() }

    private suspend fun getAllConcepts(): MutableSet<Concept> {
        return conceptRepository.getConcepts().toMutableSet()
    }
}
