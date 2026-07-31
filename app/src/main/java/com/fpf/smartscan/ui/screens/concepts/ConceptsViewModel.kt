package com.fpf.smartscan.ui.screens.concepts

import android.app.Application
import android.content.Context.MODE_PRIVATE
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fpf.smartscan.core.concepts.Concept
import com.fpf.smartscan.core.concepts.ConceptManager
import com.fpf.smartscan.core.concepts.getAllowedClusters
import com.fpf.smartscan.core.concepts.getAllowedTags
import com.fpf.smartscan.core.concepts.setAllowedClusters
import com.fpf.smartscan.core.concepts.setAllowedTags
import com.fpf.smartscan.constants.PrefsNames
import com.fpf.smartscan.core.models.ModelRepository
import com.fpf.smartscan.core.data.tags.TagRepository
import com.fpf.smartscan.core.data.clusters.ClusterMetadataRepository
import com.fpf.smartscan.core.media.CollectionType
import com.fpf.smartscan.core.media.MediaCollection
import com.fpf.smartscan.ui.action.ConceptAction
import com.fpf.smartscan.ui.state.ConceptsState
import com.fpf.smartscan.ui.utils.SelectionUtils
import com.fpf.smartscansdk.core.embeddings.toQInt8Embed
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
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
    private val conceptManager: ConceptManager,
    private val modelRepository: ModelRepository
) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "ConceptsViewModel"
    }

    private val sharedPrefs by lazy { application.getSharedPreferences(PrefsNames.APP_PREFS, MODE_PRIVATE)    }

    private val textEmbedder by lazy { modelRepository.getMiniLmTextEmbedder() }

    private val _state = MutableStateFlow(ConceptsState())
    val state: StateFlow<ConceptsState> = _state

    val concepts: StateFlow<List<Concept>> = conceptManager.allConceptsFlow
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

    val hasSelectCollection: Boolean
        get() = _state.value.collectionsSelection.selectedItems.isNotEmpty()

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
            is ConceptAction.EditConcept -> editConcept(action.newDescription)
            is ConceptAction.AddConcept -> addConcept(action.description)
            is ConceptAction.SetConceptToView -> setConceptToView(action.concept)
            is ConceptAction.ToggleSelectedConcept -> toggleSelectedConcept(action.concept)
            is ConceptAction.ToggleViewAllConcepts -> toggleViewAllConcepts()
            is ConceptAction.SetAllowedCollections -> setAllowedCollections()
            is ConceptAction.SetCollectionType -> setCollectionType(action.collectionType)
            is ConceptAction.ToggleSelectedCollection -> toggleSelectedCollection(action.collection)
            is ConceptAction.PinUnpinConcept -> pinOrUnpinConcepts()
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
            try {
                _state.update { it.copy(loading = true) }
                if(!textEmbedder.isInitialized()) textEmbedder.initialize()
                val rawDescriptionEmbedding = textEmbedder.embed(description)
                conceptManager.createConcept(description, rawDescriptionEmbedding.toQInt8Embed())
            }finally {
                _state.update { it.copy(loading = false) }
            }
        }
    }

    private fun editConcept(newDescription: String){
        val concept = _state.value.selection.selectedItems.firstOrNull()?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _state.update { it.copy(loading = true) }
                if(!textEmbedder.isInitialized()) textEmbedder.initialize()
                val rawDescriptionEmbedding = textEmbedder.embed(newDescription)
                resetSelection()
                conceptManager.editConcept(concept.copy(description = newDescription), rawDescriptionEmbedding.toQInt8Embed())
            }finally {
                _state.update { it.copy(loading = false) }
            }

        }
    }

    private fun deleteConcepts(){
        viewModelScope.launch(Dispatchers.IO) {
            val concepts = getSelectedConcepts()
            conceptManager.deleteConcepts(concepts.toList())
            resetSelection()
        }
    }

    private fun pinOrUnpinConcepts(){
        viewModelScope.launch(Dispatchers.IO) {
            val concepts = getSelectedConcepts()
            conceptManager.pinOrUnpinConcepts(concepts.toList())
            resetSelection()
        }
    }

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
            CollectionType.CLUSTER -> setAllowedClusters(sharedPrefs, selectedCollections.filter{it.type == currentState.selectedCollectionType}.map{it.id}.toSet())
            CollectionType.TAG -> setAllowedTags(sharedPrefs, selectedCollections.filter{it.type == currentState.selectedCollectionType}.map{it.id}.toSet())
            else -> {}
        }
        setCollectionType(null)
    }


    private suspend fun getSelectedConcepts(): Set<Concept> = SelectionUtils.getSelectedItems(_state.value.selection) { getAllConcepts() }

    private suspend fun getAllConcepts(): MutableSet<Concept> {
        return conceptManager.allConceptsFlow.first().toMutableSet()
    }

}
