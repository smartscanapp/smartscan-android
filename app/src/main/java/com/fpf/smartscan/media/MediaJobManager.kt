package com.fpf.smartscan.media

import android.util.Log
import com.fpf.smartscan.concepts.ConceptManager
import com.fpf.smartscan.data.mappers.toMetadata
import com.fpf.smartscan.data.media.MediaMetadataRepository
import com.fpf.smartscan.index.CloudImageIndexListener
import com.fpf.smartscan.models.ModelRepository
import com.fpf.smartscan.queue.Queue
import com.fpf.smartscan.queue.jobs.MediaProcessingJob
import com.fpf.smartscansdk.core.embeddings.StoredEmbedding
import com.fpf.smartscansdk.core.embeddings.toQInt8Embed
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class MediaJobManager(
    private val conceptManager: ConceptManager,
    private val mediaMetadataRepository: MediaMetadataRepository,
    private val modelRepository: ModelRepository,
) {

    companion object {
        private const val TAG = "MediaJobManager"
    }
    private val textEmbedder by lazy { modelRepository.getMiniLmTextEmbedder() }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val queue = Queue<MediaProcessingJob>(
        concurrency = 4,
        scope = scope,
        onProcess = { jobData, workerId ->
            when(jobData){
                is MediaProcessingJob.UpdateConceptLinks -> {
                    Log.d(TAG, "job type: UpdateConceptLinks| Worker $workerId processing ${jobData.updatedEmbed.id}")
                    conceptManager.updateConceptLinks(jobData.updatedEmbed, jobData.mediaType)
                }
                is MediaProcessingJob.UpdateDescriptionAndConceptLinks -> {
                    Log.d(TAG, "Job type: UpdateDescriptionAndConceptLinks| Worker $workerId processing ${jobData.updatedMedia.id}")
                    updateDescriptionAndConceptLinksJob(jobData.updatedMedia)
                }
            }
        }
    )
    init{
        scope.launch {
            CloudImageIndexListener.jobs.collect { job ->
                queue.submit(job)
            }
        }
    }

    fun updateDescription(updatedMedia: MediaItem){
        queue.submit(MediaProcessingJob.UpdateDescriptionAndConceptLinks(updatedMedia))
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

    private suspend fun createAndUpdateDescriptionEmbed(updatedMedia: MediaItem): StoredEmbedding? {
        if (!textEmbedder.isInitialized()) textEmbedder.initialize()
        val mediaConceptEmbedStore = conceptManager.getMediaConceptEmbedStore(updatedMedia.type)

        if (updatedMedia.description.isNullOrBlank()) return null
        val newRawEmbedding = textEmbedder.embed(updatedMedia.description)
        val existingMediaEmbed = mediaConceptEmbedStore.get(listOf(updatedMedia.id)).firstOrNull()
        val updatedOrNewMediaEmbed = existingMediaEmbed?.copy(embedding = newRawEmbedding.toQInt8Embed())
            ?: StoredEmbedding(updatedMedia.id, updatedMedia.dateAdded, newRawEmbedding.toQInt8Embed())
        if (existingMediaEmbed != null) {
            mediaConceptEmbedStore.update(listOf(updatedOrNewMediaEmbed))
        } else {
            mediaConceptEmbedStore.add(listOf(updatedOrNewMediaEmbed))
        }
        return updatedOrNewMediaEmbed
    }
}