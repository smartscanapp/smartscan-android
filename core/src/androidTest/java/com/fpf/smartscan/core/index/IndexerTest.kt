package com.fpf.smartscan.core.index

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.fpf.smartscan.core.data.MediaDatabase
import com.fpf.smartscan.core.data.TestDatabase
import com.fpf.smartscan.core.data.TestEmbedStores
import com.fpf.smartscan.core.media.MediaMetadata
import com.fpf.smartscan.core.media.MediaType
import com.fpf.smartscan.core.utils.randomEmbedding
import com.fpf.smartscansdk.core.embeddings.Embedding
import com.fpf.smartscansdk.core.embeddings.EmbeddingStore
import com.fpf.smartscansdk.core.embeddings.StoredEmbedding
import com.fpf.smartscansdk.core.embeddings.toF32Embed
import com.fpf.smartscansdk.core.embeddings.toQInt8Embed
import com.fpf.smartscansdk.core.processors.BatchProcessor
import com.fpf.smartscansdk.core.processors.MemoryOptions
import com.fpf.smartscansdk.core.processors.ProcessorListener
import com.fpf.smartscansdk.core.processors.ProcessorResult
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class IndexerTest {
    private lateinit var stores: TestEmbedStores.Stores
    private lateinit var database: MediaDatabase

    @Before
    fun setup() {
        stores = TestEmbedStores.create()
        database = TestDatabase.create()
    }

    @After
    fun tearDown() {
        stores.directory.deleteRecursively()
        database.close()
    }

    @Test
    fun test_onBatchComplete_executes_even_if_indexing_cancelled() = runTest {
        val media = createMedia(10_000)
        val batchSize = 10
        val cancelSignal = CompletableDeferred<Unit>()

        val indexer = TestIndexer(
            context = ApplicationProvider.getApplicationContext(),
            store = stores.image,
            batchSize = batchSize,
            onBatchCompleteCallback = { batch ->
                if (batch.any { it.first.id == 0L }) {
                    cancelSignal.complete(Unit)
                }
            }
        )

        val job = launch {
            indexer.index(media)
        }
        cancelSignal.await()
        job.cancelAndJoin()

        assertEquals(batchSize, stores.image.get().size)
    }

    private fun createMedia(count: Int): List<MediaMetadata> =
        List(count) { index ->
            MediaMetadata(
                id = index.toLong(),
                dateAdded = System.currentTimeMillis() + index + 1,
                type = MediaType.IMAGE
            )
        }
}

private class TestIndexer(
    private val store: EmbeddingStore,
    private val quantize: Boolean = true,
    private val onBatchCompleteCallback: (List<Pair<MediaMetadata, Embedding>>) -> Unit,
    context: Context,
    listener: ProcessorListener<MediaMetadata>? = null,
    memoryOptions: MemoryOptions = MemoryOptions(),
    batchSize: Int = 10
) : BatchProcessor<MediaMetadata, Pair<MediaMetadata, Embedding>>(context, listener, memoryOptions, batchSize) {
    override suspend fun onBatchComplete(context:Context, batch: List<Pair<MediaMetadata, Embedding>>) {
        val embedsToStore = batch.map {
            StoredEmbedding(it.first.id, it.first.dateAdded, it.second)
        }

        withContext(NonCancellable) {
            store.add(embedsToStore)
            onBatchCompleteCallback(batch)
        }
    }

    override suspend fun onProcess(context: Context, item: MediaMetadata): Pair<MediaMetadata, Embedding> {
        val output = randomEmbedding(quantize, 512)
        val embedding = if (quantize) output.toQInt8Embed() else output.toF32Embed()
        return item to embedding
    }

    suspend fun index(mediaList: List<MediaMetadata>): ProcessorResult = run(mediaList)
}