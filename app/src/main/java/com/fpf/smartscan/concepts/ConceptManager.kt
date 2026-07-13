package com.fpf.smartscan.concepts

import android.util.Log
import com.fpf.smartscan.data.concepts.ConceptCrossRefRepository
import com.fpf.smartscan.data.concepts.ConceptRepository
import com.fpf.smartscan.media.MediaItem
import com.fpf.smartscan.media.MediaType
import com.fpf.smartscansdk.core.embeddings.FileEmbeddingStore
import com.fpf.smartscansdk.core.embeddings.StoredEmbedding
import com.fpf.smartscansdk.core.embeddings.TextEmbeddingProvider
import com.fpf.smartscansdk.core.embeddings.dot
import com.fpf.smartscansdk.core.embeddings.toQInt8Embed

class ConceptManager(
    private val similarityThreshold: Float,
    private val textEmbedder: TextEmbeddingProvider,
    private val conceptRepository: ConceptRepository,
    private val conceptCrossRefRepository: ConceptCrossRefRepository,
    private val conceptEmbedStore: FileEmbeddingStore,
    private val imageConceptEmbedStore: FileEmbeddingStore,
) {

    companion object {
        private const val TAG = "ConceptManager"
    }
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

    suspend fun pinOrUnpinConcepts(concepts: List<Concept>){
        conceptRepository.updateConcepts(concepts.map{it.copy(isPinned = !it.isPinned)})
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

    suspend fun checkRecentUpdatesAndUpdateConcepts(recentUpdates: Set<MediaItem>){
        if(recentUpdates.isEmpty()) return
        if(!textEmbedder.isInitialized()) textEmbedder.initialize()

//        Log.d(TAG, "Recently updated: ${recentUpdates.size}")

        val crossRefsToDelete = mutableListOf<ConceptCrossRef>()
        val crossRefsToAdd = mutableListOf<ConceptCrossRef>()
        val newMediaEmbeds = mutableListOf<StoredEmbedding>()
        val updatedMediaEmbeds = mutableListOf<StoredEmbedding>()

        for(media in recentUpdates){
            if(media.description == null) continue

            val newRawEmbedding = textEmbedder.embed(media.description)
            val existingMediaEmbed = imageConceptEmbedStore.get(listOf(media.id)).firstOrNull()
            val updatedOrNewMediaEmbed = existingMediaEmbed?.copy(embedding = newRawEmbedding.toQInt8Embed())
                ?: StoredEmbedding(media.id, media.dateAdded, newRawEmbedding.toQInt8Embed())
            if (existingMediaEmbed != null){
                updatedMediaEmbeds.add(updatedOrNewMediaEmbed)
            }else{
                newMediaEmbeds.add(updatedOrNewMediaEmbed)
            }

            // Check if the recently updated media still matches concepts it belongs to
            val linkedConceptIds = conceptRepository.getLinkedConceptIds(media.id, media.type)
            val conceptEmbeds = conceptEmbedStore.get(linkedConceptIds)
            if (conceptEmbeds.size != linkedConceptIds.size) error("Missing embeddings for some concepts")
            for (conceptEmbed in conceptEmbeds){
                val sim = conceptEmbed.embedding.toQInt8Embed().vector dot updatedOrNewMediaEmbed.embedding.toQInt8Embed().vector
                if (sim < similarityThreshold){
                    crossRefsToDelete.add(ConceptCrossRef(media.id, conceptId = conceptEmbed.id, media.type))
                }
            }

            // Check if the recently updated media matches any concepts it's not already in
            val unlinkedConceptIds = conceptRepository.getUnlinkedConceptIds(media.id, media.type)
            val unlinkedConceptEmbeds = conceptEmbedStore.get(unlinkedConceptIds)
            if (unlinkedConceptIds.size != unlinkedConceptEmbeds.size) error("Missing embeddings for some concepts")
            for (conceptEmbed in unlinkedConceptEmbeds){
                val sim = conceptEmbed.embedding.toQInt8Embed().vector dot updatedOrNewMediaEmbed.embedding.toQInt8Embed().vector
                if (sim >= similarityThreshold){
                    crossRefsToAdd.add(ConceptCrossRef(media.id, conceptId = conceptEmbed.id, media.type))
                }
            }
        }

        if(crossRefsToDelete.isNotEmpty()){
            conceptCrossRefRepository.delete(crossRefsToDelete)
        }
        if(crossRefsToAdd.isNotEmpty()){
            conceptCrossRefRepository.insertConceptCrossRefs(crossRefsToAdd)
        }
        if(newMediaEmbeds.isNotEmpty()){
            imageConceptEmbedStore.add(newMediaEmbeds)
        }
        if(updatedMediaEmbeds.isNotEmpty()){
            imageConceptEmbedStore.update(updatedMediaEmbeds)
        }
    }

    private fun generateId(): Long = System.currentTimeMillis() + idCount
}


