package com.fpf.smartscan.core.concepts

import com.fpf.smartscan.core.data.concepts.ConceptCrossRefRepository
import com.fpf.smartscan.core.data.concepts.ConceptRepository
import com.fpf.smartscan.core.media.MediaType
import com.fpf.smartscan.core.search.toSimsMap
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

    val allConceptsFlow = conceptRepository.getConceptsFlow()

    val exists = conceptEmbedStore.exists

    suspend fun createConcept(description: String, descriptionEmbed: Embedding){
        val concept = NewConcept( description = description)
        val id = conceptRepository.insertConcept(concept)

        val conceptEmbed = StoredEmbedding(id = id, date = System.currentTimeMillis(), descriptionEmbed.toQInt8Embed())
        conceptEmbedStore.add(listOf(conceptEmbed))
        findAndUpdateMediaMatchingConcept(id)
    }

    suspend fun editConcept(updatedConcept: Concept, descriptionEmbed: Embedding){
        val updatedConcept = updatedConcept.copy(updatedAt = System.currentTimeMillis())
        conceptRepository.upsertConcept(updatedConcept)

        val updatedEmbed = StoredEmbedding(id = updatedConcept.id, date = System.currentTimeMillis(), descriptionEmbed.toQInt8Embed())
        conceptEmbedStore.update(updatedEmbed)
        findAndUpdateMediaMatchingConcept(updatedConcept.id)
    }

    suspend fun deleteConcepts(concepts: List<Concept>){
        conceptRepository.deleteConcepts(concepts)
        conceptEmbedStore.remove(concepts.map{it.id})
    }

    suspend fun pinOrUnpinConcepts(concepts: List<Concept>){
        conceptRepository.updateConcepts(concepts.map{it.copy(isPinned = !it.isPinned)})
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

    suspend fun getReminderCandidates(recentSearchesEmbeddings: List<Embedding>, recentReminders: Set<Pair<Long, MediaType>> = emptySet(), topN: Int = 5): List<Pair<Long, MediaType>>{
        val matchMedia = mutableMapOf<Pair<Long, MediaType>, Float>()
        for(embed in recentSearchesEmbeddings){
            val imageResult = imageConceptEmbedStore.query(embed, Int.MAX_VALUE, similarityThreshold, includeSims = true).toSimsMap()
            val videoResult = videoConceptEmbedStore.query(embed, Int.MAX_VALUE, similarityThreshold, includeSims = true).toSimsMap()
            imageResult.forEach { (mediaId, sim) -> matchMedia.merge(Pair(mediaId, MediaType.IMAGE), sim, Float::plus) }
            videoResult.forEach { (mediaId, sim) -> matchMedia.merge(Pair(mediaId, MediaType.VIDEO), sim, Float::plus) }
        }
        return matchMedia.entries.filter{it.key !in recentReminders}.sortedByDescending { it.value }.take(topN).map{it.key}
    }

    fun getMediaConceptEmbedStore(mediaType: MediaType): FileEmbeddingStore = when(mediaType){
        MediaType.VIDEO -> videoConceptEmbedStore
        MediaType.IMAGE -> imageConceptEmbedStore
    }

    private suspend fun findAndUpdateMediaMatchingConcept(conceptId: Long){
        val mediaMatchesMap = findMediaMatchingConcept(conceptId)
        val crossrefs = mediaMatchesMap.map{ConceptCrossRef(mediaId = it.first, mediaType=it.second, conceptId = conceptId, similarity = it.third)}
        conceptCrossRefRepository.insertConceptCrossRefs(crossrefs)
    }

    private suspend fun findMediaMatchingConcept(conceptId: Long): List<Triple<Long, MediaType, Float>>{
        val matchMedia = mutableListOf<Triple<Long, MediaType, Float>>()
        val conceptEmbedding = conceptEmbedStore.get(conceptId)?: return matchMedia
        val imageResult = imageConceptEmbedStore.query(conceptEmbedding.embedding, Int.MAX_VALUE, similarityThreshold, includeSims = true).toSimsMap()
        val videoResult = videoConceptEmbedStore.query(conceptEmbedding.embedding, Int.MAX_VALUE, similarityThreshold, includeSims = true).toSimsMap()
        matchMedia.addAll(imageResult.map{Triple(it.key, MediaType.IMAGE, it.value)})
        matchMedia.addAll(videoResult.map{Triple(it.key, MediaType.VIDEO, it.value)})
        return matchMedia
    }

    private suspend fun findConceptLinksToRemove(mediaEmbed: StoredEmbedding, type: MediaType): List<ConceptCrossRef>{
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

    private suspend fun findConceptLinksToAdd(mediaEmbed: StoredEmbedding, type: MediaType): List<ConceptCrossRef>{
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
}


