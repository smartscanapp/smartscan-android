package com.fpf.smartscan.di

import android.app.Application
import com.fpf.smartscan.constants.EmbeddingStoresFilesQuant
import com.fpf.smartscansdk.core.embeddings.FileEmbeddingStore
import org.koin.core.qualifier.named
import org.koin.dsl.module
import java.io.File

private const val CLIP_EMBEDDING_DIM = 512
private const val MINILM_EMBEDDING_DIM = 384

val IMAGE_EMBED_STORE = named("image_embed_store")
val VIDEO_EMBED_STORE = named("video_embed_store")
val CLUSTER_EMBED_STORE = named("cluster_embed_store")
val CONCEPT_EMBED_STORE = named("concept_embed_store")
val CONCEPT_IMAGE_EMBED_STORE = named("concept_image_embed_store")

val embedStoreModule = module {
    single(IMAGE_EMBED_STORE) {
        val app = get<Application>()
        FileEmbeddingStore(File(app.filesDir, EmbeddingStoresFilesQuant.IMAGE), CLIP_EMBEDDING_DIM, quantize = true)
    }

    single(VIDEO_EMBED_STORE) {
        val app = get<Application>()
        FileEmbeddingStore(File(app.filesDir, EmbeddingStoresFilesQuant.VIDEO), CLIP_EMBEDDING_DIM, quantize = true)
    }
    single(CLUSTER_EMBED_STORE) {
        val app = get<Application>()
        FileEmbeddingStore(File(app.filesDir, EmbeddingStoresFilesQuant.CLUSTER), CLIP_EMBEDDING_DIM, quantize = true)
    }
    single(CONCEPT_EMBED_STORE) {
        val app = get<Application>()
        FileEmbeddingStore(File(app.filesDir, EmbeddingStoresFilesQuant.CONCEPT), MINILM_EMBEDDING_DIM, quantize = true)
    }
    single(CONCEPT_IMAGE_EMBED_STORE) {
        val app = get<Application>()
        FileEmbeddingStore(File(app.filesDir, EmbeddingStoresFilesQuant.IMAGE_CONCEPT), MINILM_EMBEDDING_DIM, quantize = true)
    }
}