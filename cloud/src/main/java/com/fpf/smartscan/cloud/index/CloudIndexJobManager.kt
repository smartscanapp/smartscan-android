package com.fpf.smartscan.cloud.index

import android.app.Application
import android.content.SharedPreferences
import com.fpf.smartscan.core.concepts.getAllowedClusters
import com.fpf.smartscan.core.concepts.getAllowedTags
import com.fpf.smartscan.core.data.media.MediaMetadataRepository
import com.fpf.smartscan.core.errors.AppException
import com.fpf.smartscan.core.media.MediaJobManager
import com.fpf.smartscan.core.media.MediaType
import com.fpf.smartscansdk.core.embeddings.FileEmbeddingStore
import com.fpf.smartscansdk.core.embeddings.TextEmbeddingProvider
import com.fpf.smartscansdk.core.processors.ProcessorResult
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
    suspend fun run(
        mediaTypes: List<MediaType>,
        apiKey: String?,
        onResult: (suspend (ProcessorResult, MediaType) -> Unit )? = null
    ): Map<MediaType, ProcessorResult>{
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
        val results = mutableMapOf<MediaType, ProcessorResult>()

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
                    val imageResult = imageIndexer.index(
                        allowedTags = allowedTags.toList(),
                        allowedClusters = allowedClusters.toList()
                    )
                    results[mediaType] = imageResult
                    onResult?.invoke(imageResult, mediaType)
                }

                MediaType.VIDEO -> {/* TODO */ }
            }
        }
        return results
    }
}