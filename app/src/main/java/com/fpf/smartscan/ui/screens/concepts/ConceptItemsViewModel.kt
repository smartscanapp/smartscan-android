package com.fpf.smartscan.ui.screens.concepts


import android.app.Application
import android.content.SharedPreferences
import android.util.Log
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.fpf.smartscan.R
import com.fpf.smartscan.core.concepts.Concept
import com.fpf.smartscan.constants.PrefsKeys
import com.fpf.smartscan.core.data.concepts.ConceptCrossRefRepository
import com.fpf.smartscan.core.data.paging.ConceptPagingSource
import com.fpf.smartscan.core.data.media.MediaMetadataRepository
import com.fpf.smartscan.core.media.MediaItem
import com.fpf.smartscan.core.media.MediaType
import com.fpf.smartscan.core.search.SortBy
import com.fpf.smartscan.events.ConceptItemEvent
import com.fpf.smartscan.events.ConceptItemEventType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ConceptItemsViewModel(
    application: Application,
    private val conceptCrossRefRepository: ConceptCrossRefRepository,
    private val mediaMetadataRepository: MediaMetadataRepository,
    private val sharedPrefs: SharedPreferences
) : AndroidViewModel(application) {
    companion object {
        private const val TAG = "ConceptItemsViewModel"
    }

    private val _state = MutableStateFlow(ConceptItemsState())
    val state: StateFlow<ConceptItemsState> = _state

    private val _event = MutableSharedFlow<ConceptItemEvent>()
    val event = _event.asSharedFlow()

    val sortByOptions: List<Pair<String, SortBy>>
        get() = listOf(
            getApplication<Application>().getString(R.string.sort_date_asc_option) to SortBy.Date(ascending = true),
            getApplication<Application>().getString(R.string.sort_date_desc_option) to SortBy.Date(ascending = false),
            getApplication<Application>().getString(R.string.sort_similarity_asc_option) to SortBy.Similarity(ascending = true),
            getApplication<Application>().getString(R.string.sort_similarity_desc_option) to SortBy.Similarity(ascending = false)
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    val conceptItems = _state
        .map { Triple(it.filter, it.sortBy, it.concept) }
        .distinctUntilChanged()
        .flatMapLatest { (filters, sortBy, concept) ->
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

    init {
        load()
    }

    fun onAction(action: ConceptItemsAction){
        when(action){
            is ConceptItemsAction.SetMediaToView -> setMediaToView(action.item)
            is ConceptItemsAction.SetConceptToView -> setConcept(action.concept)
            is ConceptItemsAction.SetMediaTypeFilter -> setMediaTypeFilter(action.mediaType)
            is ConceptItemsAction.SetShowHiddenFilter -> setShowHiddenFilter(action.showHidden)
            is ConceptItemsAction.SetSortBy -> setSortBy(action.sortBy)
            is ConceptItemsAction.ToggleHide -> toggleHide(action.item)
        }
    }

    private fun load(){
        _state.update { it.copy(sortBy = getSortByPref()) }
    }

    private fun toggleHide(item: MediaItem){
        val concept = _state.value.concept?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                conceptCrossRefRepository.setCrossRefHidden(item.id, item.type, concept.id, !item.isHidden)
                _event.emit(ConceptItemEvent(ConceptItemEventType.HIDE, success = true))
            }catch (e: Exception){
                Log.e(TAG, "Error in toggleHide", e)
                _event.emit(ConceptItemEvent(ConceptItemEventType.HIDE, success = false, message = "Failed to update item visibility"))
            }
        }
    }

    private fun setConcept(concept: Concept?) = _state.update { it.copy(concept=concept) }

    private fun setMediaToView(item: MediaItem?) = _state.update { it.copy(mediaToView =item) }
    private fun setMediaTypeFilter(mediaType: MediaType?) = _state.update { it.copy(filter = it.filter.copy(mediaType=mediaType)) }

    // Null mean include hidden. True would only show hidden
    private fun setShowHiddenFilter(showHidden: Boolean) = _state.update { it.copy(filter = it.filter.copy(showHidden = if(showHidden) null else false)) }

    private fun setSortBy(sortBy: SortBy) {
        _state.update { it.copy(sortBy = sortBy) }
        saveSortByPref(sortBy)
    }

    private fun saveSortByPref(sortBy: SortBy) {
        val option = sortByOptions.find { it.second == sortBy }?.second ?: sortByOptions.first().second
        sharedPrefs.edit {
            putString(PrefsKeys.SORT_BY_CONCEPT_ITEMS, option.toString())
        }
    }

    private fun getSortByPref(): SortBy {
        val sortByStr = sharedPrefs.getString(PrefsKeys.SORT_BY_CONCEPT_ITEMS, "") ?: ""
        return sortByOptions.find { it.second.toString() == sortByStr }?.second ?: SortBy.Date()
    }

}
