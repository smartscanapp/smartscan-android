package com.fpf.smartscan.concepts

import com.fpf.smartscan.data.concepts.ConceptCrossRefRepository
import com.fpf.smartscan.data.concepts.ConceptRepository
import com.fpf.smartscan.media.MediaType
import com.fpf.smartscansdk.core.embeddings.FileEmbeddingStore

class ConceptManager(
    private val conceptRepository: ConceptRepository,
    private val conceptCrossRefRepository: ConceptCrossRefRepository,
    private val conceptEmbedStore: FileEmbeddingStore,
    private val imageConceptEmbedStore: FileEmbeddingStore,
) {
    private var idCount: Long = 0L

    suspend fun createConcept(description: String): Concept{
        val concept = Concept(id = generateId(), description = description, size = 0)
        conceptRepository.insertConcept(concept)
        ++idCount
        return concept
    }

    suspend fun updateConcept(concept: Concept, newDescription: String){
        val updatedConcept = concept.copy(description = newDescription, updatedAt = System.currentTimeMillis())
        conceptRepository.updateConcept(updatedConcept)
    }

    suspend fun deleteConcepts(concepts: List<Concept>){
        conceptRepository.deleteConcepts(concepts)
    }

    suspend fun findMediaMatchingConcept(concept: Concept, threshold: Float): Map<Long, MediaType>{
        val conceptEmbedding = conceptEmbedStore.get(listOf(concept.id)).firstOrNull()?: return emptyMap()
        val result = imageConceptEmbedStore.query(conceptEmbedding.embedding, Int.MAX_VALUE, threshold)
        return result.ids.associateWith { MediaType.IMAGE } // will add video support later
    }

    suspend fun findAndUpdateMediaMatchingConcept(concept: Concept, threshold: Float){
        val mediaMatchesMap = findMediaMatchingConcept(concept, threshold)
        addMediaToConcept(mediaMatchesMap, concept.id)
    }

    suspend fun addMediaToConcept(mediaMatchesMap: Map<Long, MediaType>, conceptId: Long){
        val crossrefs = mediaMatchesMap.map{ConceptCrossRef(mediaId = it.key, mediaType=it.value, conceptId = conceptId)}
        conceptCrossRefRepository.insertConceptCrossRefs(crossrefs)
    }

    private fun generateId(): Long = System.currentTimeMillis() + idCount
}


