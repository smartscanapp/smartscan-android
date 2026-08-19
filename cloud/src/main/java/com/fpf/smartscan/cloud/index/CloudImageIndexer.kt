package com.fpf.smartscan.cloud.index

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import com.fpf.smartscan.core.data.media.MediaMetadataRepository
import com.fpf.smartscan.core.jobs.MediaProcessingJob
import com.fpf.smartscan.core.media.MediaJobManager
import com.fpf.smartscan.core.media.MediaMetadata
import com.fpf.smartscan.core.media.MediaType
import com.fpf.smartscan.core.utils.uriToBase64
import com.fpf.smartscansdk.core.embeddings.StoredEmbedding
import com.fpf.smartscansdk.core.embeddings.EmbeddingStore
import com.fpf.smartscansdk.core.embeddings.TextEmbeddingProvider
import com.fpf.smartscansdk.core.embeddings.toF32Embed
import com.fpf.smartscansdk.core.embeddings.toQInt8Embed
import com.fpf.smartscansdk.core.processors.BatchProcessor
import com.fpf.smartscansdk.core.processors.ProcessorListener
import com.fpf.smartscansdk.core.processors.MemoryOptions
import com.fpf.smartscansdk.core.processors.ProcessorResult
import com.fpflabs.llmconnect.openai.OpenaiClient
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable

class CloudImageIndexer(
    context: Context,
    private val embedder: TextEmbeddingProvider,
    private val openaiClient: OpenaiClient,
    private val store: EmbeddingStore,
    private val mediaJobManager: MediaJobManager,
    private val mediaMetadataRepository: MediaMetadataRepository,
    private val quantize: Boolean,
    private val maxImageSize: Int = 720,
    listener: ProcessorListener<MediaMetadata>? = null,
    memoryOptions: MemoryOptions = MemoryOptions(),
    batchSize: Int = 10,
): BatchProcessor<MediaMetadata, Pair<MediaMetadata, StoredEmbedding>?>(context, listener, memoryOptions, batchSize){


    override suspend fun onBatchComplete(context: Context, batch: List<Pair<MediaMetadata, StoredEmbedding>?>) {
        val filteredBatch = batch.filterNotNull()
        withContext(NonCancellable){
            store.add(filteredBatch.map{it.second})
            mediaMetadataRepository.update(filteredBatch.map { it.first })
            filteredBatch.forEach { (metadata, embed) ->
                mediaJobManager.enqueue(MediaProcessingJob.UpdateConceptLinks(embed, metadata.type))
            }
        }
    }

    override suspend fun onProcess(context: Context, item: MediaMetadata): Pair<MediaMetadata, StoredEmbedding>? {
        val contentUri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, item.id)
        val base64 = uriToBase64(context, contentUri, maxImageSize)
        val result = openaiClient.generateJsonFromImage(DEFAULT_PROMPT, base64, ImageSummary.serializer())
        if( result.summary.isBlank()) return null
        val formatted = formatOutput(result)
        val rawEmbedding =  embedder.embed(formatted)
        val embed = if(quantize) rawEmbedding.toQInt8Embed() else rawEmbedding.toF32Embed()
        val storedEmbedding = StoredEmbedding(item.id, item.dateAdded, embed)
        val updatedMetadata = item.copy(description = result.summary)
        return Pair(updatedMetadata, storedEmbedding)
    }

    suspend fun index(allowedTags: List<Long>, allowedClusters: List<Long>): ProcessorResult{
        val mediaType = MediaType.IMAGE
        val mediaProcess = mutableSetOf<MediaMetadata>()

        if(allowedTags.isNotEmpty()) {
            val existingMediaMatchingTags = mediaMetadataRepository.getByTagsWithoutDescription(allowedTags, mediaType)
            mediaProcess.addAll(existingMediaMatchingTags)
        }
        if(allowedClusters.isNotEmpty()) {
            val existingMediaMatchingClusters = mediaMetadataRepository.getByClustersWithoutDescription(allowedClusters, mediaType)
            mediaProcess.addAll(existingMediaMatchingClusters)
        }
        return run(mediaProcess.toList())
    }

    private fun formatOutput(output: ImageSummary): String {
        val topicsStr = "[TOPICS]: ${output.topics.joinToString(", ")}."
        val summary = "[SUMMARY]: ${output.summary}"
        return topicsStr + "\n" + summary
    }
}

@Serializable
data class ImageSummary(
    val summary: String,
    val topics: List<String>
)