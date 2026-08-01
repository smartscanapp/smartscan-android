package com.fpf.smartscan.core.concepts

import com.fpf.smartscan.core.data.concepts.ConceptCrossRefRepository
import com.fpf.smartscan.core.data.concepts.ConceptRepository
import com.fpf.smartscan.core.media.MediaType
import com.fpf.smartscan.core.search.Reranker
import com.fpf.smartscan.core.search.toSimsMap
import com.fpf.smartscansdk.core.embeddings.Embedding
import com.fpf.smartscansdk.core.embeddings.FileEmbeddingStore
import com.fpf.smartscansdk.core.embeddings.StoredEmbedding
import com.fpf.smartscansdk.core.embeddings.dot
import com.fpf.smartscansdk.core.embeddings.toQInt8Embed
import kotlin.math.max

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

    private val conceptToThresholdMap: MutableMap<Long, Double> = mutableMapOf()

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
        conceptToThresholdMap.remove(updatedConcept.id)

        findAndUpdateMediaMatchingConcept(updatedConcept)
    }

    suspend fun deleteConcepts(concepts: List<Concept>){
        conceptRepository.deleteConcepts(concepts)
        conceptEmbedStore.remove(concepts.map{it.id})
        for(c in concepts) conceptToThresholdMap.remove(c.id)
    }

    suspend fun pinOrUnpinConcepts(concepts: List<Concept>){
        conceptRepository.updateConcepts(concepts.map{it.copy(isPinned = !it.isPinned)})
    }

    suspend fun findAndUpdateMediaMatchingConcept(concept: Concept){
        val mediaMatchesMap = findMediaMatchingConcept(concept)
        val crossrefs = mediaMatchesMap.map{ConceptCrossRef(mediaId = it.key.first, mediaType=it.key.second, conceptId = concept.id, similarity = it.value)}
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

    fun getMediaConceptEmbedStore(mediaType: MediaType): FileEmbeddingStore = when(mediaType){
        MediaType.VIDEO -> videoConceptEmbedStore
        MediaType.IMAGE -> imageConceptEmbedStore
    }

    private suspend fun findMediaMatchingConcept(concept: Concept): Map<Pair<Long, MediaType>, Float>{
        val conceptEmbedding = conceptEmbedStore.get(listOf(concept.id)).firstOrNull()?: return emptyMap()
        val imageResult = query(conceptEmbedding.embedding, imageConceptEmbedStore)
        val videoResult = query(conceptEmbedding.embedding, videoConceptEmbedStore)
        val mediaItemSimsMap = imageResult
            .mapKeys { (id, _) -> id to MediaType.IMAGE } + videoResult.mapKeys { (id, _) -> id to MediaType.VIDEO }
        return mediaItemSimsMap
    }

    private suspend fun query(queryEmbed: Embedding, store: FileEmbeddingStore): Map<Long, Float>{
        val result = store.query(queryEmbed, Int.MAX_VALUE, similarityThreshold, includeSims = true)
        val simsMap = result.toSimsMap()
        val cutOff = Reranker.calculateRelevanceCutoff(simsMap)
        return simsMap.filter { it.value >= cutOff }
    }


    private suspend fun findConceptLinksToRemove(mediaEmbed: StoredEmbedding, type: MediaType): MutableList<ConceptCrossRef>{
        val crossRefsToDelete = mutableListOf<ConceptCrossRef>()
        val linkedConceptIds = conceptRepository.getLinkedConceptIds(mediaEmbed.id, type)
        val conceptEmbeds = conceptEmbedStore.get(linkedConceptIds)
        if (conceptEmbeds.size != linkedConceptIds.size) error("Missing embeddings for some concepts")
        val conceptScores = getConceptToScoresMap(linkedConceptIds.filterNot { it in conceptToThresholdMap })
        conceptScores.forEach { (conceptId, scores) -> conceptToThresholdMap[conceptId] = Reranker.calculateRelevanceCutoff(scores) }
        for (conceptEmbed in conceptEmbeds){
            val sim = conceptEmbed.embedding.toQInt8Embed().vector dot mediaEmbed.embedding.toQInt8Embed().vector
            val dynamicThreshold = conceptToThresholdMap[conceptEmbed.id]?.toFloat()
            val threshold = max(similarityThreshold, dynamicThreshold?: similarityThreshold)
            if (sim < threshold){
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
        val conceptScores = getConceptToScoresMap(unlinkedConceptIds.filterNot { it in conceptToThresholdMap })
        conceptScores.forEach { (conceptId, scores) -> conceptToThresholdMap[conceptId] = Reranker.calculateRelevanceCutoff(scores) }
        for (conceptEmbed in unlinkedConceptEmbeds){
            val sim = conceptEmbed.embedding.toQInt8Embed().vector dot mediaEmbed.embedding.toQInt8Embed().vector
            val dynamicThreshold = conceptToThresholdMap[conceptEmbed.id]?.toFloat()
            val threshold = max(similarityThreshold, dynamicThreshold?: similarityThreshold)
            if (sim >= threshold){
                crossRefsToAdd.add(ConceptCrossRef(mediaEmbed.id, conceptId = conceptEmbed.id, type, sim))
            }
        }
        return crossRefsToAdd
    }

    suspend fun getConceptToScoresMap(ids: List<Long>): Map<Long, Map<Long, Float>> {
        return conceptCrossRefRepository.getByConceptIds(ids)
            .groupBy { it.conceptId }
            .mapValues { (_, crossRefs) ->
                crossRefs.associate { it.mediaId to it.similarity }
            }
    }

    private fun generateId(): Long = System.currentTimeMillis() + idCount
}


