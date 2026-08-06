package com.fpf.smartscan.core.index

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import com.fpf.smartscan.core.media.MediaMetadata
import com.fpf.smartscansdk.core.embeddings.Embedding
import com.fpf.smartscansdk.core.embeddings.StoredEmbedding
import com.fpf.smartscansdk.core.embeddings.EmbeddingStore
import com.fpf.smartscansdk.core.embeddings.ImageEmbeddingProvider
import com.fpf.smartscansdk.core.embeddings.toQInt8
import com.fpf.smartscansdk.core.media.getBitmapFromUri
import com.fpf.smartscansdk.core.processors.BatchProcessor
import com.fpf.smartscansdk.core.processors.ProcessorListener
import com.fpf.smartscansdk.core.processors.MemoryOptions
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

class ImageIndexer(
    private val embedder: ImageEmbeddingProvider,
    private val store: EmbeddingStore,
    private val maxImageSize: Int = 225,
    private val quantize: Boolean = false,
    context: Context,
    listener: ProcessorListener<MediaMetadata>? = null,
    memoryOptions: MemoryOptions = MemoryOptions(),
    batchSize: Int = 10,
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
        val contentUri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, item.id)
        val bitmap = getBitmapFromUri(context, contentUri, maxImageSize)
        val output = withContext(NonCancellable) { embedder.embed(bitmap) }
        val embedding = if(quantize) Embedding.QInt8(output.toQInt8()) else Embedding.F32(output)
        return Pair(item, embedding)
    }
}