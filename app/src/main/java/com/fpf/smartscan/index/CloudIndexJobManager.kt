package com.fpf.smartscan.index

import android.app.Application
import android.content.SharedPreferences
import android.util.Log
import com.fpf.smartscan.R
import com.fpf.smartscan.api.llm.LLMProviderConfig
import com.fpf.smartscan.api.llm.OpenaiClient
import com.fpf.smartscan.data.metadata.MediaMetadataRepository
import com.fpf.smartscan.media.MediaType
import com.fpf.smartscan.concepts.getAllowedClusters
import com.fpf.smartscan.concepts.getAllowedTags
import com.fpf.smartscan.constants.DEFAULT_SYSTEM_PROMPT
import com.fpf.smartscan.errors.AppException
import com.fpf.smartscan.media.MediaMetadata
import com.fpf.smartscan.settings.loadSettings
import com.fpf.smartscan.utils.showNotification
import com.fpf.smartscansdk.core.embeddings.Embedding
import com.fpf.smartscansdk.core.embeddings.FileEmbeddingStore
import com.fpf.smartscansdk.core.embeddings.StoredEmbedding
import com.fpf.smartscansdk.core.embeddings.TextEmbeddingProvider
import com.fpf.smartscansdk.core.processors.BatchProcessor
import kotlinx.coroutines.CancellationException

class CloudIndexJobManager(
    private val application: Application,
    private val sharedPrefs: SharedPreferences,
    private val textEmbedder: TextEmbeddingProvider,
    private val imageConceptsEmbedStore: FileEmbeddingStore,
    private val mediaMetadataRepository: MediaMetadataRepository,
    private val useListener: Boolean = true
) {
    companion object {
        private const val TAG = "CloudIndexJobManager"
        private const val NOTIFICATION_ID = 200
    }

    // TextEmbedder used here is not closed because it's a singleton as model is required throughout app.
    suspend fun run(mediaTypes: List<MediaType>){
        try {
            val appSettings = loadSettings(sharedPrefs)
            val allowedTags= getAllowedTags(sharedPrefs)
            val allowedClusters = getAllowedClusters(sharedPrefs)
            val openaiClient = OpenaiClient(
                apiKey = appSettings.openaiApiKey?: throw AppException.MissingApiKey("Missing OpenAI API key"),
                config = LLMProviderConfig(model = "gpt-5.4-mini", systemPrompt = DEFAULT_SYSTEM_PROMPT, maxTokens = 500)
            )
            if(!textEmbedder.isInitialized()) textEmbedder.initialize()

            mediaTypes.forEach { mediaType ->
                when (mediaType) {
                    MediaType.IMAGE -> {
                        val imageIndexer = CloudImageIndexer(
                            context = application,
                            embedder=textEmbedder,
                            listener = if(useListener) CloudImageIndexListener else null,
                            store = imageConceptsEmbedStore,
                            mediaMetadataRepository = mediaMetadataRepository,
                            quantize = true,
                            openaiClient = openaiClient
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
        catch (e: AppException.MissingApiKey) {
            Log.e(TAG, e.message, e)
            val title = application.getString(R.string.notif_title_index_error_service, "Media")
            val content = application.getString(R.string.notif_content_missing_api_key_error_service)
            showNotification(application, title, content, NOTIFICATION_ID + 1)
        }

        catch (e: CancellationException) {
            Log.w(TAG, "Indexing job cancelled:", e)
        }
        catch (e: Exception) {
            Log.e(TAG, "Cloud Indexing failed:", e)
            val title = application.getString(R.string.notif_title_index_error_service, "Media")
            val content = application.getString(R.string.notif_content_index_error_service)
            showNotification(application, title, content, NOTIFICATION_ID + 1)
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