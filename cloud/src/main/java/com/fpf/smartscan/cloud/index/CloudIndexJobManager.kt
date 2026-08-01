package com.fpf.smartscan.cloud.index

import android.app.Application
import android.content.SharedPreferences
import com.fpf.smartscan.core.concepts.getAllowedClusters
import com.fpf.smartscan.core.concepts.getAllowedTags
import com.fpf.smartscan.core.data.media.MediaMetadataRepository
import com.fpf.smartscan.core.errors.AppException
import com.fpf.smartscan.core.media.MediaJobManager
import com.fpf.smartscan.core.media.MediaMetadata
import com.fpf.smartscan.core.media.MediaType
import com.fpf.smartscansdk.core.embeddings.FileEmbeddingStore
import com.fpf.smartscansdk.core.embeddings.StoredEmbedding
import com.fpf.smartscansdk.core.embeddings.TextEmbeddingProvider
import com.fpf.smartscansdk.core.processors.BatchProcessor
import com.fpflabs.llmconnect.LLMProviderConfig
import com.fpflabs.llmconnect.openai.OpenaiClient

class CloudIndexJobManager(
    private val application: Application,
    private val sharedPrefs: SharedPreferences,
    private val textEmbedder: TextEmbeddingProvider,
    private val imageConceptsEmbedStore: FileEmbeddingStore,
    private val mediaMetadataRepository: MediaMetadataRepository,
    private val mediaJobManager: MediaJobManager,
    private val useListener: Boolean = true
) {
    companion object {
        private const val TAG = "CloudIndexJobManager"
    }

    // TextEmbedder used here is not closed because it's a singleton as model is required throughout app.
    suspend fun run(mediaTypes: List<MediaType>, apiKey: String?){
        val allowedTags= getAllowedTags(sharedPrefs)
        val allowedClusters = getAllowedClusters(sharedPrefs)
        val openaiClient = OpenaiClient(
            apiKey = apiKey ?: throw AppException.MissingApiKey(),
            config = LLMProviderConfig(
                model = "gpt-5.4-mini",
                systemPrompt = DEFAULT_SYSTEM_PROMPT,
                maxTokens = 500
            )
        )
        if(!textEmbedder.isInitialized()) textEmbedder.initialize()

        mediaTypes.forEach { mediaType ->
            when (mediaType) {
                MediaType.IMAGE -> {
                    val imageIndexer = CloudImageIndexer(
                        context = application,
                        openaiClient = openaiClient,
                        embedder=textEmbedder,
                        listener = if(useListener) CloudImageIndexListener else null,
                        store = imageConceptsEmbedStore,
                        mediaMetadataRepository = mediaMetadataRepository,
                        mediaJobManager=mediaJobManager,
                        quantize = true,
                    )
                    indexMediaCloud(
                        mediaType,
                        imageIndexer,
                        mediaMetadataRepository,
                        allowedTags = allowedTags.toList(),
                        allowedClusters = allowedClusters.toList()
                    )
                }

                MediaType.VIDEO -> {
                    // TODO
                }
            }
        }
    }

    // For usage with concepts - may allow general usage some point in the future if necessary
    // But this would involve architectural changes
    private suspend fun indexMediaCloud(
        mediaType: MediaType,
        indexer: BatchProcessor<MediaMetadata, Pair<MediaMetadata, StoredEmbedding>?>,
        metadataRepo: MediaMetadataRepository,
        allowedTags: List<Long>,
        allowedClusters: List<Long>,
    ){
        val mediaProcess = mutableSetOf<MediaMetadata>()

        if(allowedTags.isNotEmpty()) {
            val existingMediaMatchingTags = metadataRepo.getByTagsWithoutDescription(allowedTags, mediaType)
            mediaProcess.addAll(existingMediaMatchingTags)
        }
        if(allowedClusters.isNotEmpty()) {
            val existingMediaMatchingClusters = metadataRepo.getByClustersWithoutDescription(allowedClusters, mediaType)
            mediaProcess.addAll(existingMediaMatchingClusters)
        }
        indexer.run(mediaProcess.toList())
    }

}