package com.fpf.smartscan.ui.state

import com.fpf.smartscan.core.concepts.Concept
import com.fpf.smartscan.core.media.CollectionType
import com.fpf.smartscan.core.media.MediaCollection
import com.fpf.smartscan.ui.state.common.SelectionState

data class ConceptsState(
    val selectedCollectionType: CollectionType? = null,
    val showAllConcepts: Boolean = false,
    val loading: Boolean = false,
    val conceptToView: Concept? = null,
    val totalConcepts: Int = 0,
    val totalCollections: Int = 0,
    val collectionsSelection: SelectionState<MediaCollection> = SelectionState(),
    val selection: SelectionState<Concept> = SelectionState()
)