package com.fpf.smartscan.index


import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import android.util.Log
import com.fpf.smartscan.api.ImageSummary
import com.fpf.smartscan.api.llm.OpenaiClient
import com.fpf.smartscan.concepts.HighlightsCodec
import com.fpf.smartscan.constants.DEFAULT_PROMPT
import com.fpf.smartscan.data.metadata.MediaMetadataRepository
import com.fpf.smartscan.media.MediaMetadata
import com.fpf.smartscan.utils.uriToBase64
import com.fpf.smartscansdk.core.embeddings.Embedding
import com.fpf.smartscansdk.core.embeddings.StoredEmbedding
import com.fpf.smartscansdk.core.embeddings.EmbeddingStore
import com.fpf.smartscansdk.core.embeddings.TextEmbeddingProvider
import com.fpf.smartscansdk.core.embeddings.toF32Embed
import com.fpf.smartscansdk.core.embeddings.toQInt8Embed
import com.fpf.smartscansdk.core.processors.BatchProcessor
import com.fpf.smartscansdk.core.processors.ProcessorListener
import com.fpf.smartscansdk.core.processors.MemoryOptions
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

class ConceptsImageIndexer(
    context: Context,
    private val embedder: TextEmbeddingProvider,
    private val openaiClient: OpenaiClient,
    private val store: EmbeddingStore,
    private val mediaMetadataRepository: MediaMetadataRepository,
    private val quantize: Boolean,
    private val maxImageSize: Int = 720,
    listener: ProcessorListener<MediaMetadata, Pair<MediaMetadata, Embedding>?>? = null,
    memoryOptions: MemoryOptions = MemoryOptions(),
    batchSize: Int = 10,
): BatchProcessor<MediaMetadata, Pair<MediaMetadata, Embedding>?>(context, listener, memoryOptions, batchSize){


    override suspend fun onBatchComplete(context: Context, batch: List<Pair<MediaMetadata, Embedding>?>) {
        val filteredBatch = batch.filterNotNull()
        val metadataList = filteredBatch.map{it.first}
        val imageIdToDateMap = getImageToDateMap(context, metadataList.map { it.id })
        val embedsToStore = filteredBatch.map{
            val date = imageIdToDateMap[it.first.id]?: System.currentTimeMillis()
            StoredEmbedding(it.first.id, date, it.second)
        }
        store.add(embedsToStore)
        mediaMetadataRepository.update(metadataList)
        listener?.onBatchComplete(context, batch)
    }

    override suspend fun onProcess(context: Context, item: MediaMetadata): Pair<MediaMetadata, Embedding>?? {
        val contentUri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, item.id)
        val base64 = uriToBase64(context, contentUri, maxImageSize)
        val result = openaiClient.generateJsonFromImage(DEFAULT_PROMPT, base64, ImageSummary.serializer())
        Log.d(TAG, "LLM Output: ${result.toString()}")
        if(!result.isTextBasedImage || result.highlights.isEmpty()) return null
        val highlightsAsString = HighlightsCodec.encode(result.highlights)
        val rawEmbedding = withContext(NonCancellable) { embedder.embed(highlightsAsString) }
        val embed = if(quantize) rawEmbedding.toQInt8Embed() else rawEmbedding.toF32Embed()
        val updatedMetadata = item.copy(description = highlightsAsString)
        return Pair(updatedMetadata, embed)
    }

    private fun getImageToDateMap(context: Context, ids: List<Long>): Map<Long, Long> {
        val result = mutableMapOf<Long, Long>()
        val uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DATE_ADDED
        )

        val chunkSize = 500

        ids.chunked(chunkSize).forEach { chunk ->

            val selection = "${MediaStore.Images.Media._ID} IN (${
                chunk.joinToString(",")
            })"

            context.applicationContext.contentResolver.query(
                uri,
                projection,
                selection,
                null,
                null
            )?.use { cursor ->

                val idIdx = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val dateIdx = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)

                while (cursor.moveToNext()) {
                    result[cursor.getLong(idIdx)] = cursor.getLong(dateIdx)
                }
            }
        }
        return result
    }
}