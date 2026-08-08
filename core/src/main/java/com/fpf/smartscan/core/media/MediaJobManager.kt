package com.fpf.smartscan.core.media

import android.util.Log
import com.fpf.smartscan.core.concepts.ConceptManager
import com.fpf.smartscan.core.data.mappers.toMetadata
import com.fpf.smartscan.core.data.media.MediaMetadataRepository
import com.fpf.smartscan.core.jobs.MediaProcessingJob
import com.fpf.smartscan.core.jobs.Queue
import com.fpf.smartscan.core.models.ModelRepository
import com.fpf.smartscansdk.core.embeddings.Embedding
import com.fpf.smartscansdk.core.embeddings.FileEmbeddingStore
import com.fpf.smartscansdk.core.embeddings.StoredEmbedding
import com.fpf.smartscansdk.core.embeddings.dot
import com.fpf.smartscansdk.core.embeddings.toQInt8Embed
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MediaJobManager(
    private val conceptManager: ConceptManager,
    private val mediaMetadataRepository: MediaMetadataRepository,
    private val modelRepository: ModelRepository,
    private val imageEmbedStore: FileEmbeddingStore,
    private val videoEmbedStore: FileEmbeddingStore,
    private val imageConceptEmbedStore: FileEmbeddingStore,
    private val videoConceptEmbedStore: FileEmbeddingStore,
) {

    companion object {
        private const val TAG = "MediaJobManager"
        private const val DEDUPE_THRESHOLD = 0.985f
    }
    private val textEmbedder by lazy { modelRepository.getMiniLmTextEmbedder() }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val queue = Queue<MediaProcessingJob>(
        concurrency = 4,
        scope = scope,
        onProcess = { jobData, _ ->
            when(jobData){
                is MediaProcessingJob.UpdateConceptLinks -> {
                    conceptManager.updateConceptLinks(jobData.updatedEmbed, jobData.mediaType)
                }
                is MediaProcessingJob.UpdateDescriptionAndConceptLinks -> {
                    updateDescriptionAndConceptLinksJob(jobData.updatedMedia)
                }
            }
        }
    )
    fun enqueue(job: MediaProcessingJob) = queue.submit(job)

    fun updateDescription(updatedMedia: MediaItem){
        queue.submit(MediaProcessingJob.UpdateDescriptionAndConceptLinks(updatedMedia))
    }

    fun delete(ids: List<Long>, mediaType: MediaType){
        val stores = getEmbedStores(mediaType)
        scope.launch(Dispatchers.IO) {
            removeStaleMedia(ids, mediaType, stores, mediaMetadataRepository)
        }
    }

    private suspend fun updateDescriptionAndConceptLinksJob(updatedMedia: MediaItem){
        mediaMetadataRepository.update(listOf(updatedMedia.toMetadata()))

        val updatedEmbed = createAndUpdateDescriptionEmbed(updatedMedia)
        if (updatedEmbed == null) {
            conceptManager.deleteConceptLinks(updatedMedia.id, updatedMedia.type)
            return
        }
        conceptManager.updateConceptLinks(updatedEmbed, updatedMedia.type)
    }


    fun findAndMarkDuplicates(mediaType: MediaType){
        scope.launch {
            val nonDuplicateEmbeds = mutableListOf<Embedding>()
            val duplicateIds = mutableListOf<Long>()
            val store = getVlmEmbedStore(mediaType)
            val storedEmbeds = store.get().sortedBy { it.date }

            for (storedEmbed in storedEmbeds) {
                var isDuplicate = false
                for (emb in nonDuplicateEmbeds) {
                    val sim = when (emb) {
                        is Embedding.F32 -> (storedEmbed.embedding as Embedding.F32).vector dot emb.vector
                        is Embedding.QInt8 -> (storedEmbed.embedding as Embedding.QInt8).vector dot emb.vector
                    }
                    if (sim >= DEDUPE_THRESHOLD) {
                        isDuplicate = true
                        break
                    }
                }
                if (isDuplicate) {
                    duplicateIds.add(storedEmbed.id)
                } else {
                    nonDuplicateEmbeds.add(storedEmbed.embedding)
                }
            }

            if (duplicateIds.isNotEmpty()) {
                mediaMetadataRepository.markDuplicates(duplicateIds, mediaType)
            }
        }
    }

    private suspend fun createAndUpdateDescriptionEmbed(updatedMedia: MediaItem): StoredEmbedding? {
        if (!textEmbedder.isInitialized()) textEmbedder.initialize()
        val mediaConceptEmbedStore = conceptManager.getMediaConceptEmbedStore(updatedMedia.type)

        if (updatedMedia.description.isNullOrBlank()) return null
        val newRawEmbedding = withContext(Dispatchers.Default){
            textEmbedder.embed(updatedMedia.description)
        }
        val existingMediaEmbed = mediaConceptEmbedStore.get(listOf(updatedMedia.id)).firstOrNull()
        val updatedOrNewMediaEmbed = existingMediaEmbed?.copy(embedding = newRawEmbedding.toQInt8Embed())
            ?: StoredEmbedding(
                updatedMedia.id,
                updatedMedia.dateAdded,
                newRawEmbedding.toQInt8Embed()
            )
        if (existingMediaEmbed != null) {
            mediaConceptEmbedStore.update(listOf(updatedOrNewMediaEmbed))
        } else {
            mediaConceptEmbedStore.add(listOf(updatedOrNewMediaEmbed))
        }
        return updatedOrNewMediaEmbed
    }

    private fun getEmbedStores(mediaType: MediaType) = when(mediaType){
        MediaType.IMAGE -> listOf(imageEmbedStore, imageConceptEmbedStore)
        MediaType.VIDEO -> listOf(videoEmbedStore, videoConceptEmbedStore)
    }

    private fun getVlmEmbedStore(mediaType: MediaType) = when(mediaType){
        MediaType.IMAGE -> imageEmbedStore
        MediaType.VIDEO -> videoEmbedStore
    }
}