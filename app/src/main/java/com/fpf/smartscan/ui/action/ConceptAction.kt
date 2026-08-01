package com.fpf.smartscan.ui.action

import com.fpf.smartscan.core.concepts.Concept
import com.fpf.smartscan.core.media.CollectionType
import com.fpf.smartscan.core.media.MediaCollection

sealed interface ConceptAction {
    data class SetCollectionType(val collectionType: CollectionType?): ConceptAction
    data class ToggleSelectedConcept(val concept: Concept): ConceptAction
    data class ToggleSelectedCollection(val collection: MediaCollection): ConceptAction
    data class SetConceptToView(val concept: Concept?): ConceptAction
    data class SetSelectAll(val selectAll: Boolean): ConceptAction
    data class AddConcept(val description: String) : ConceptAction
    data class EditConcept(val newDescription: String): ConceptAction
    data object PinUnpinConcept: ConceptAction
    data object DeleteConcepts : ConceptAction
    data object ToggleViewAllConcepts: ConceptAction
    data object ToggleSelectionMode: ConceptAction
    data object ClearSelection: ConceptAction
    data object ResetSelection: ConceptAction
    data object SetAllowedCollections: ConceptAction

}