package com.fpf.smartscan.core.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.fpf.smartscansdk.core.embeddings.FileEmbeddingStore
import java.io.File

object TestEmbedStores {
    private const val CLIP_EMBED_DIM = 512
    private const val MINILM_EMBED_DIM = 384

    fun create(): Stores {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val directory = context.cacheDir.resolve("test-${System.nanoTime()}")
        directory.mkdirs()

        return Stores(
            image = FileEmbeddingStore(directory.resolve("image.bin"), CLIP_EMBED_DIM, quantize = true),
            video = FileEmbeddingStore(directory.resolve("video.bin"), CLIP_EMBED_DIM, quantize = true),
            cluster = FileEmbeddingStore(directory.resolve("cluster.bin"), CLIP_EMBED_DIM, quantize = true),
            concept = FileEmbeddingStore(directory.resolve("concept.bin"), MINILM_EMBED_DIM, quantize = true),
            imageConcept = FileEmbeddingStore(directory.resolve("image_concept.bin"), MINILM_EMBED_DIM, quantize = true),
            videoConcept = FileEmbeddingStore(directory.resolve("video_concept.bin"), MINILM_EMBED_DIM, quantize = true),
            directory = directory
        )
    }

    data class Stores(
        val image: FileEmbeddingStore,
        val video: FileEmbeddingStore,
        val cluster: FileEmbeddingStore,
        val concept: FileEmbeddingStore,
        val imageConcept: FileEmbeddingStore,
        val videoConcept: FileEmbeddingStore,
        val directory: File
    )
}