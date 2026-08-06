package com.fpf.smartscan.core.index

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import com.fpf.smartscan.core.media.MediaMetadata
import com.fpf.smartscansdk.core.embeddings.Embedding
import com.fpf.smartscansdk.core.embeddings.StoredEmbedding
import com.fpf.smartscansdk.core.embeddings.EmbeddingStore
import com.fpf.smartscansdk.core.embeddings.ImageEmbeddingProvider
import com.fpf.smartscansdk.core.embeddings.embedBatch
import com.fpf.smartscansdk.core.embeddings.generatePrototypeEmbedding
import com.fpf.smartscansdk.core.embeddings.toQInt8
import com.fpf.smartscansdk.core.processors.BatchProcessor
import com.fpf.smartscansdk.core.media.extractFramesFromVideo
import com.fpf.smartscansdk.core.processors.ProcessorListener
import com.fpf.smartscansdk.core.processors.MemoryOptions
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

class VideoIndexer(
    private val embedder: ImageEmbeddingProvider,
    private val frameCount: Int = 10,
    private val width: Int,
    private val height: Int,
    private val quantize: Boolean = false,
    context: Context,
    listener: ProcessorListener<MediaMetadata>? = null,
    batchSize: Int = 10,
    memoryOptions: MemoryOptions = MemoryOptions(),
    private val store: EmbeddingStore,
): BatchProcessor<MediaMetadata, Pair<MediaMetadata, Embedding>>(context, listener, memoryOptions, batchSize){

    override suspend fun onBatchComplete(context: Context, batch: List<Pair<MediaMetadata, Embedding>>) {
        val embedsToStore = batch.map{
            StoredEmbedding(it.first.id, it.first.dateAdded, it.second)
        }
        // NonCancellable required to avoid file corruption if coroutine cancelled
        withContext(NonCancellable){
            store.add(embedsToStore)
        }
    }

    override suspend fun onProcess(context: Context, item: MediaMetadata): Pair<MediaMetadata, Embedding> {
        val contentUri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, item.id)
        val frameBitmaps = extractFramesFromVideo(context, contentUri, width = width, height = height, frameCount = frameCount)
        val rawEmbeddings = embedBatch(context, embedder, frameBitmaps)
        val output: FloatArray = generatePrototypeEmbedding(rawEmbeddings)
        val embedding = if(quantize) Embedding.QInt8(output.toQInt8()) else Embedding.F32(output)
        return Pair(item, embedding)
    }
}