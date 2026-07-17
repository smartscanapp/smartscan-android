package com.fpf.smartscan.ui.shared

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fpf.smartscan.concepts.ConceptManager
import com.fpf.smartscan.data.concepts.ConceptCrossRefRepository
import com.fpf.smartscan.data.concepts.ConceptRepository
import com.fpf.smartscan.data.mappers.toMetadata
import com.fpf.smartscan.data.metadata.MediaMetadataRepository
import com.fpf.smartscan.events.MediaEvent
import com.fpf.smartscan.events.MediaEventType
import com.fpf.smartscan.media.MediaItem
import com.fpf.smartscan.media.MediaType
import com.fpf.smartscan.models.ModelRepository
import com.fpf.smartscan.utils.Queue
import com.fpf.smartscansdk.core.embeddings.FileEmbeddingStore
import com.fpf.smartscansdk.core.embeddings.StoredEmbedding
import com.fpf.smartscansdk.core.embeddings.toQInt8Embed
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MediaViewModel(
    private val imageConceptEmbedStore: FileEmbeddingStore,
    private val videoConceptEmbedStore: FileEmbeddingStore,
    private val mediaMetadataRepository: MediaMetadataRepository,
    private val conceptEmbedStore: FileEmbeddingStore,
    private val conceptRepository: ConceptRepository,
    private val conceptCrossRefRepository: ConceptCrossRefRepository,
    private val modelRepository: ModelRepository,
) : ViewModel() {
    companion object {
        private const val TAG = "MediaViewModel"
    }

    private val textEmbedder by lazy { modelRepository.getMiniLmTextEmbedder() }

    private val queue = Queue<MediaEvent>(
        concurrency = 4,
        scope = viewModelScope,
        onProcess = { jobData, workerId ->
            Log.d(TAG, "Worker $workerId processing ${jobData.updatedEmbed.id}")
            conceptManager.updateConceptLinks(jobData.updatedEmbed, jobData.mediaType)
        }
    )

    val conceptManager by lazy {
        ConceptManager(
            conceptRepository = conceptRepository,
            conceptCrossRefRepository = conceptCrossRefRepository,
            conceptEmbedStore = conceptEmbedStore,
            imageConceptEmbedStore = imageConceptEmbedStore,
            videoConceptEmbedStore = videoConceptEmbedStore
        )
    }

    init {
        viewModelScope.launch(Dispatchers.Default) {
            mediaMetadataRepository.event.collect{
                event -> handleEvents(event)
            }
        }
    }

    fun saveUpdatedItem(updatedMedia: MediaItem){
        viewModelScope.launch (Dispatchers.IO) {
            mediaMetadataRepository.update(listOf(updatedMedia.toMetadata()))

            // Fire-forget
            viewModelScope.launch(Dispatchers.Default){
                val updatedEmbed = updateMediaDescriptionEmbed(updatedMedia)
                if(updatedEmbed == null){
                    deleteStaleConceptEmbed(updatedMedia.id, updatedMedia.type)
                    return@launch
                }
                val result = conceptManager.updateConceptLinks(updatedEmbed, updatedMedia.type)
                Log.d(TAG, "removed: ${result.removed} concept links | added: ${result.added} concept links")
            }
        }
        Log.d(TAG, "Updated media description for: ${updatedMedia.id}")
    }

    private suspend fun deleteStaleConceptEmbed(mediaStoreId: Long, type: MediaType) {
        conceptCrossRefRepository.delete(mediaStoreId, type)
        val mediaConceptEmbedStore = conceptManager.getMediaConceptEmbedStore(type)
        mediaConceptEmbedStore.remove(listOf(mediaStoreId))
    }


    private suspend fun updateMediaDescriptionEmbed(updatedMedia: MediaItem): StoredEmbedding? {
        if (!textEmbedder.isInitialized()) textEmbedder.initialize()
        val mediaConceptEmbedStore = conceptManager.getMediaConceptEmbedStore(updatedMedia.type)

        if (updatedMedia.description.isNullOrBlank()) return null
        val newRawEmbedding = textEmbedder.embed(updatedMedia.description)
        //TODO: add `upsert` method to EmbeddingStore
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

    private suspend fun handleEvents(event: MediaEvent){
        when(event.eventType){
            MediaEventType.EMBED_UPDATE -> {
                queue.submit(event)
            }
        }
    }
}