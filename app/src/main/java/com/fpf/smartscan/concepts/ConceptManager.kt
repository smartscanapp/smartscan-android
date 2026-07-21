package com.fpf.smartscan.concepts

import android.util.Log
import com.fpf.smartscan.data.concepts.ConceptCrossRefRepository
import com.fpf.smartscan.data.concepts.ConceptRepository
import com.fpf.smartscan.media.MediaType
import com.fpf.smartscan.search.toSimsMap
import com.fpf.smartscansdk.core.embeddings.Embedding
import com.fpf.smartscansdk.core.embeddings.FileEmbeddingStore
import com.fpf.smartscansdk.core.embeddings.StoredEmbedding
import com.fpf.smartscansdk.core.embeddings.dot
import com.fpf.smartscansdk.core.embeddings.toQInt8Embed

class ConceptManager(
    private val conceptRepository: ConceptRepository,
    private val conceptCrossRefRepository: ConceptCrossRefRepository,
    private val conceptEmbedStore: FileEmbeddingStore,
    private val imageConceptEmbedStore: FileEmbeddingStore,
    private val videoConceptEmbedStore: FileEmbeddingStore,
    private val similarityThreshold: Float = DEFAULT_SIMILARITY_THRESHOLD,
    ) {

    companion object {
        private const val TAG = "ConceptManager"
        const val DEFAULT_SIMILARITY_THRESHOLD = 0.25f
    }

    data class ConceptUpdateLinksResult(
        val removed: Int,
        val added: Int
    )

    private var idCount: Long = 0L

    val allConceptsFlow = conceptRepository.getConceptsFlow()

    suspend fun createConcept(description: String, descriptionEmbed: Embedding){
        val concept = Concept(id = generateId(), description = description, size = 0)
        conceptRepository.insertConcept(concept)

        val conceptEmbed = StoredEmbedding(id = concept.id, date = System.currentTimeMillis(), descriptionEmbed.toQInt8Embed())
        conceptEmbedStore.add(listOf(conceptEmbed))

        ++idCount
        findAndUpdateMediaMatchingConcept(concept)
    }

    suspend fun editConcept(updatedConcept: Concept, descriptionEmbed: Embedding){
        val updatedConcept = updatedConcept.copy(updatedAt = System.currentTimeMillis())
        conceptRepository.upsertConcept(updatedConcept)

        val updatedEmbed = StoredEmbedding(id = updatedConcept.id, date = System.currentTimeMillis(), descriptionEmbed.toQInt8Embed())
        conceptEmbedStore.update(listOf(updatedEmbed))

        findAndUpdateMediaMatchingConcept(updatedConcept)
    }

    suspend fun deleteConcepts(concepts: List<Concept>){
        conceptRepository.deleteConcepts(concepts)
        conceptEmbedStore.remove(concepts.map{it.id})
    }

    suspend fun pinOrUnpinConcepts(concepts: List<Concept>){
        conceptRepository.updateConcepts(concepts.map{it.copy(isPinned = !it.isPinned)})
    }

    suspend fun findAndUpdateMediaMatchingConcept(concept: Concept){
        val mediaMatchesMap = findMediaMatchingConcept(concept)
        addMediaToConcept(mediaMatchesMap, concept.id)
    }

    suspend fun addMediaToConcept(mediaMatchesMap:Map<Pair<Long, MediaType>, Float>, conceptId: Long){
        val crossrefs = mediaMatchesMap.map{ConceptCrossRef(mediaId = it.key.first, mediaType=it.key.second, conceptId = conceptId, similarity = it.value)}
        conceptCrossRefRepository.insertConceptCrossRefs(crossrefs)
    }

    suspend fun updateConceptLinks(mediaEmbed: StoredEmbedding, type: MediaType): ConceptUpdateLinksResult{
        // Check if the recently updated media still matches concepts it belongs to
        val crossRefsToDelete = findConceptLinksToRemove( mediaEmbed, type)
        // Check if the recently updated media matches any concepts it's not already in
        val crossRefsToAdd = findConceptLinksToAdd( mediaEmbed, type)

        if(crossRefsToDelete.isNotEmpty()){
            conceptCrossRefRepository.delete(crossRefsToDelete)
        }
        if(crossRefsToAdd.isNotEmpty()){
            conceptCrossRefRepository.insertConceptCrossRefs(crossRefsToAdd)
        }

        return ConceptUpdateLinksResult(
            removed = crossRefsToDelete.size,
            added = crossRefsToAdd.size
        )
    }

    suspend fun deleteConceptLinks(mediaStoreId: Long, type: MediaType) {
        conceptCrossRefRepository.delete(mediaStoreId, type)
        val mediaConceptEmbedStore = getMediaConceptEmbedStore(type)
        mediaConceptEmbedStore.remove(listOf(mediaStoreId))
    }

    private suspend fun findMediaMatchingConcept(concept: Concept): Map<Pair<Long, MediaType>, Float>{
        val conceptEmbedding = conceptEmbedStore.get(listOf(concept.id)).firstOrNull()?: return emptyMap()
        val imageResult = query(conceptEmbedding.embedding, imageConceptEmbedStore, MediaType.IMAGE)
        val videoResult = query(conceptEmbedding.embedding, videoConceptEmbedStore, MediaType.VIDEO)
        val mediaItemSimsMap =  imageResult + videoResult
        return mediaItemSimsMap
    }

    private suspend fun query(queryEmbed: Embedding, store: FileEmbeddingStore, mediaType: MediaType): Map<Pair<Long, MediaType>, Float>{
        val imageResults = store.query(queryEmbed, Int.MAX_VALUE, similarityThreshold, includeSims = true)
        val imagesSimsMap = imageResults.toSimsMap()
       return buildMap {
           imagesSimsMap.map { put(Pair(it.key, mediaType), it.value)}
       }
    }

    private suspend fun findConceptLinksToRemove(mediaEmbed: StoredEmbedding, type: MediaType): MutableList<ConceptCrossRef>{
        val crossRefsToDelete = mutableListOf<ConceptCrossRef>()
        val linkedConceptIds = conceptRepository.getLinkedConceptIds(mediaEmbed.id, type)
        val conceptEmbeds = conceptEmbedStore.get(linkedConceptIds)
        if (conceptEmbeds.size != linkedConceptIds.size) error("Missing embeddings for some concepts")
        for (conceptEmbed in conceptEmbeds){
            val sim = conceptEmbed.embedding.toQInt8Embed().vector dot mediaEmbed.embedding.toQInt8Embed().vector
            if (sim < similarityThreshold){
                crossRefsToDelete.add(ConceptCrossRef(mediaEmbed.id, conceptId = conceptEmbed.id, type, sim))
            }
        }
        return crossRefsToDelete
    }


    private suspend fun findConceptLinksToAdd(mediaEmbed: StoredEmbedding, type: MediaType): MutableList<ConceptCrossRef>{
        val crossRefsToAdd = mutableListOf<ConceptCrossRef>()
        val unlinkedConceptIds = conceptRepository.getUnlinkedConceptIds(mediaEmbed.id, type)
        val unlinkedConceptEmbeds = conceptEmbedStore.get(unlinkedConceptIds)
        if (unlinkedConceptIds.size != unlinkedConceptEmbeds.size) error("Missing embeddings for some concepts")
        for (conceptEmbed in unlinkedConceptEmbeds){
            val sim = conceptEmbed.embedding.toQInt8Embed().vector dot mediaEmbed.embedding.toQInt8Embed().vector
            if (sim >= similarityThreshold){
                crossRefsToAdd.add(ConceptCrossRef(mediaEmbed.id, conceptId = conceptEmbed.id, type, sim))
            }
        }
        return crossRefsToAdd
    }

    fun getMediaConceptEmbedStore(mediaType: MediaType): FileEmbeddingStore = when(mediaType){
        MediaType.VIDEO -> videoConceptEmbedStore
        MediaType.IMAGE -> imageConceptEmbedStore
    }

    private fun generateId(): Long = System.currentTimeMillis() + idCount
}


