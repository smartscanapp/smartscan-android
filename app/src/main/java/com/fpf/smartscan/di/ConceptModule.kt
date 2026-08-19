package com.fpf.smartscan.di

import com.fpf.smartscan.core.concepts.ConceptManager
import org.koin.dsl.module

val conceptModule = module {
    single {
        ConceptManager(
            conceptRepository = get(),
            conceptCrossRefRepository = get(),
            conceptEmbedStore = get(CONCEPT_EMBED_STORE),
            imageConceptEmbedStore = get(CONCEPT_IMAGE_EMBED_STORE),
            videoConceptEmbedStore = get(CONCEPT_VIDEO_EMBED_STORE)
        )
    }
}
