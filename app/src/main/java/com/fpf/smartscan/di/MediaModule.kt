package com.fpf.smartscan.di

import com.fpf.smartscan.core.media.MediaJobManager
import org.koin.dsl.module

val mediaModule = module {
    single {
        MediaJobManager(
            conceptManager = get(),
            mediaMetadataRepository = get(),
            modelRepository = get(),
            imageEmbedStore = get(IMAGE_EMBED_STORE),
            videoEmbedStore = get(VIDEO_EMBED_STORE),
            imageConceptEmbedStore = get(CONCEPT_IMAGE_EMBED_STORE),
            videoConceptEmbedStore = get(CONCEPT_VIDEO_EMBED_STORE)
        )
    }
}
