package com.fpf.smartscan.ui.action

import com.fpf.smartscan.concepts.Concept
import com.fpf.smartscan.media.CollectionType
import com.fpf.smartscan.media.MediaCollection

sealed interface ConceptAction {
    data class SetCollectionType(val collectionType: CollectionType?): ConceptAction
    data class ToggleSelectedConcept(val concept: Concept): ConceptAction
    data class ToggleSelectedCollection(val collection: MediaCollection): ConceptAction
    data class SetConceptToView(val concept: Concept?): ConceptAction
    data class SetSelectAll(val selectAll: Boolean): ConceptAction
    data class AddConcept(val description: String) : ConceptAction
    data class UpdateConcept(val concept: Concept, val newDescription: String): ConceptAction
    data object DeleteConcepts : ConceptAction
    data object ToggleViewAllConcepts: ConceptAction
    data object ToggleSelectionMode: ConceptAction
    data object ClearSelection: ConceptAction
    data object ResetSelection: ConceptAction
    data object SetAllowedCollections: ConceptAction

}