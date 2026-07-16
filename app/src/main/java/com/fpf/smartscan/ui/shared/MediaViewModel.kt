package com.fpf.smartscan.ui.shared

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fpf.smartscan.concepts.ConceptManager
import com.fpf.smartscan.data.concepts.ConceptCrossRefRepository
import com.fpf.smartscan.data.concepts.ConceptRepository
import com.fpf.smartscan.data.mappers.toMetadata
import com.fpf.smartscan.data.metadata.MediaMetadataRepository
import com.fpf.smartscan.media.MediaItem
import com.fpf.smartscan.media.MediaType
import com.fpf.smartscan.models.ModelRepository
import com.fpf.smartscansdk.core.embeddings.FileEmbeddingStore
import com.fpf.smartscansdk.core.embeddings.StoredEmbedding
import com.fpf.smartscansdk.core.embeddings.toQInt8Embed
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MediaViewModel(
    application: Application,
    private val imageConceptEmbedStore: FileEmbeddingStore,
    private val videoConceptEmbedStore: FileEmbeddingStore,
    private val mediaMetadataRepository: MediaMetadataRepository,
    private val conceptEmbedStore: FileEmbeddingStore,
    private val conceptRepository: ConceptRepository,
    private val conceptCrossRefRepository: ConceptCrossRefRepository,
    private val modelRepository: ModelRepository,
) : AndroidViewModel(application) {

    private val textEmbedder by lazy { modelRepository.getMiniLmTextEmbedder() }

    val conceptManager by lazy {
        ConceptManager(
            textEmbedder = textEmbedder,
            conceptRepository = conceptRepository,
            conceptCrossRefRepository = conceptCrossRefRepository,
            conceptEmbedStore = conceptEmbedStore,
            imageConceptEmbedStore = imageConceptEmbedStore
        )
    }

    private fun saveUpdatedItem(updatedMedia: MediaItem){
        viewModelScope.launch (Dispatchers.IO) {
            mediaMetadataRepository.update(listOf(updatedMedia.toMetadata()))
            // Fire forget
            viewModelScope.launch(Dispatchers.Default){
                updateMediaDescriptionEmbed(updatedMedia)
            }
        }
    }


    private suspend fun updateMediaDescriptionEmbed(updatedMedia: MediaItem): StoredEmbedding? {
        if (!textEmbedder.isInitialized()) textEmbedder.initialize()
        if (updatedMedia.description == null) return null

        val newRawEmbedding = textEmbedder.embed(updatedMedia.description)
        val existingMediaEmbed = imageConceptEmbedStore.get(listOf(updatedMedia.id)).firstOrNull()
        val updatedOrNewMediaEmbed = existingMediaEmbed?.copy(embedding = newRawEmbedding.toQInt8Embed())
                ?: StoredEmbedding(updatedMedia.id, updatedMedia.dateAdded, newRawEmbedding.toQInt8Embed())
        if (existingMediaEmbed != null) {
            when(updatedMedia.type){
                MediaType.VIDEO -> videoConceptEmbedStore.update(listOf(updatedOrNewMediaEmbed))
                MediaType.IMAGE -> imageConceptEmbedStore.update(listOf(updatedOrNewMediaEmbed))
            }
        } else {
            when(updatedMedia.type){
                MediaType.VIDEO -> videoConceptEmbedStore.add(listOf(updatedOrNewMediaEmbed))
                MediaType.IMAGE -> imageConceptEmbedStore.add(listOf(updatedOrNewMediaEmbed))
            }
        }
        return updatedOrNewMediaEmbed
    }
}