package com.fpf.smartscan.concepts

import com.fpf.smartscan.data.concepts.ConceptCrossRefRepository
import com.fpf.smartscan.data.concepts.ConceptRepository
import com.fpf.smartscan.media.MediaType
import com.fpf.smartscansdk.core.embeddings.FileEmbeddingStore
import com.fpf.smartscansdk.core.embeddings.StoredEmbedding
import com.fpf.smartscansdk.core.embeddings.TextEmbeddingProvider
import com.fpf.smartscansdk.core.embeddings.toQInt8Embed

class ConceptManager(
    private val similarityThreshold: Float,
    private val textEmbedder: TextEmbeddingProvider,
    private val conceptRepository: ConceptRepository,
    private val conceptCrossRefRepository: ConceptCrossRefRepository,
    private val conceptEmbedStore: FileEmbeddingStore,
    private val imageConceptEmbedStore: FileEmbeddingStore,
) {
    private var idCount: Long = 0L

    suspend fun createConcept(description: String){
        if(!textEmbedder.isInitialized()) textEmbedder.initialize()
        val rawEmbedding = textEmbedder.embed(description)
        val concept = Concept(id = generateId(), description = description, size = 0)
        conceptRepository.insertConcept(concept)

        val conceptEmbed = StoredEmbedding(id = concept.id, date = System.currentTimeMillis(), rawEmbedding.toQInt8Embed())
        conceptEmbedStore.add(listOf(conceptEmbed))

        ++idCount
        findAndUpdateMediaMatchingConcept(concept)
    }

    suspend fun editConcept(concept: Concept, newDescription: String){
        if(!textEmbedder.isInitialized()) textEmbedder.initialize()
        val updatedConcept = concept.copy(description = newDescription, updatedAt = System.currentTimeMillis())
        conceptRepository.upsertConcept(updatedConcept)

        val rawEmbedding = textEmbedder.embed(newDescription)
        val updatedEmbed = StoredEmbedding(id = updatedConcept.id, date = System.currentTimeMillis(), rawEmbedding.toQInt8Embed())
        conceptEmbedStore.update(listOf(updatedEmbed))

        findAndUpdateMediaMatchingConcept(updatedConcept)
    }

    suspend fun deleteConcepts(concepts: List<Concept>){
        conceptRepository.deleteConcepts(concepts)
        conceptEmbedStore.remove(concepts.map{it.id})
    }

    suspend fun findMediaMatchingConcept(concept: Concept): Map<Long, MediaType>{
        val conceptEmbedding = conceptEmbedStore.get(listOf(concept.id)).firstOrNull()?: return emptyMap()
        val result = imageConceptEmbedStore.query(conceptEmbedding.embedding, Int.MAX_VALUE, similarityThreshold)
        return result.ids.associateWith { MediaType.IMAGE } // will add video support later
    }

    suspend fun findAndUpdateMediaMatchingConcept(concept: Concept){
        val mediaMatchesMap = findMediaMatchingConcept(concept)
        addMediaToConcept(mediaMatchesMap, concept.id)
    }

    suspend fun addMediaToConcept(mediaMatchesMap: Map<Long, MediaType>, conceptId: Long){
        val crossrefs = mediaMatchesMap.map{ConceptCrossRef(mediaId = it.key, mediaType=it.value, conceptId = conceptId)}
        conceptCrossRefRepository.insertConceptCrossRefs(crossrefs)
    }

    private fun generateId(): Long = System.currentTimeMillis() + idCount
}


