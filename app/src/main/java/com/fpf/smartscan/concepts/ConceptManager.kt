package com.fpf.smartscan.concepts

import com.fpf.smartscan.data.concepts.ConceptCrossRefRepository
import com.fpf.smartscan.data.concepts.ConceptRepository
import com.fpf.smartscan.media.MediaType
import com.fpf.smartscansdk.core.embeddings.FileEmbeddingStore
import com.fpf.smartscansdk.core.embeddings.StoredEmbedding
import com.fpf.smartscansdk.core.embeddings.TextEmbeddingProvider
import com.fpf.smartscansdk.core.embeddings.toQInt8Embed

class ConceptManager(
    private val textEmbedder: TextEmbeddingProvider,
    private val conceptRepository: ConceptRepository,
    private val conceptCrossRefRepository: ConceptCrossRefRepository,
    private val conceptEmbedStore: FileEmbeddingStore,
    private val imageConceptEmbedStore: FileEmbeddingStore,
) {
    private var idCount: Long = 0L

    suspend fun createConcept(description: String): Concept{
        if(!textEmbedder.isInitialized()) textEmbedder.initialize()
        val rawEmbedding = textEmbedder.embed(description)
        val concept = Concept(id = generateId(), description = description, size = 0)
        conceptRepository.insertConcept(concept)
        conceptEmbedStore.add(listOf(StoredEmbedding(id = concept.id, date = System.currentTimeMillis(), rawEmbedding.toQInt8Embed())))
        ++idCount
        return concept
    }

    suspend fun editConcept(concept: Concept, newDescription: String): Concept{
        val updatedConcept = concept.copy(description = newDescription, updatedAt = System.currentTimeMillis())
        conceptRepository.upsertConcept(updatedConcept)
        return updatedConcept
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


