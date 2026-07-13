package com.fpf.smartscan.di

import org.koin.core.module.dsl.viewModel
import com.fpf.smartscan.MainViewModel
import com.fpf.smartscan.ui.screens.collections.CollectionItemsViewModel
import com.fpf.smartscan.ui.screens.collections.CollectionsViewModel
import com.fpf.smartscan.ui.screens.concepts.ConceptItemsViewModel
import com.fpf.smartscan.ui.screens.concepts.ConceptsViewModel
import com.fpf.smartscan.ui.screens.search.SearchViewModel
import org.koin.dsl.module

val viewModelModule = module {

    viewModel {
        MainViewModel(
            application = get(),
            db = get(),
            imageStore = get(IMAGE_EMBED_STORE),
            videoStore = get(VIDEO_EMBED_STORE),
            clusterStore = get(CLUSTER_EMBED_STORE),
            clusterCrossRefRepository = get(),
            clusterMetadataRepository = get()
        )
    }
    viewModel {
        SearchViewModel(
            application = get(),
            imageEmbedStore = get(IMAGE_EMBED_STORE),
            videoEmbedStore = get(VIDEO_EMBED_STORE),
            clusterEmbedStore = get(CLUSTER_EMBED_STORE),
            mediaMetadataRepository = get(),
            tagRepository = get(),
            tagCrossRefRepository = get(),
            clusterCrossRefRepository = get(),
            clusterMetadataRepository = get()
        )
    }
    viewModel {
        CollectionItemsViewModel(
            application = get(),
            imageEmbedStore = get(IMAGE_EMBED_STORE),
            videoEmbedStore = get(VIDEO_EMBED_STORE),
            mediaMetadataRepository = get(),
            tagRepository = get(),
            tagCrossRefRepository = get(),
            clusterCrossRefRepository = get(),
            clusterMetadataRepository = get(),
            clusterEmbedStore = get(CLUSTER_EMBED_STORE)
        )
    }

    viewModel {
        CollectionsViewModel(
            application = get(),
            mediaMetadataRepository = get(),
            tagRepository = get(),
            tagCrossRefRepository = get(),
            clusterCrossRefRepository = get(),
            clusterMetadataRepository = get(),
            imageEmbedStore = get(IMAGE_EMBED_STORE),
            videoEmbedStore = get(VIDEO_EMBED_STORE),
            clusterEmbedStore = get(CLUSTER_EMBED_STORE)
        )
    }
    viewModel {
        ConceptsViewModel(
            application = get(),
            tagRepository = get(),
            clusterMetadataRepository = get(),
            conceptRepository = get(),
            conceptCrossRefRepository = get(),
            mediaMetadataRepository = get(),
            conceptEmbedStore = get(CONCEPT_EMBED_STORE),
            imageConceptEmbedStore = get(CONCEPT_IMAGE_EMBED_STORE)
        )
    }
    viewModel {
        ConceptItemsViewModel(
            application = get(),
            mediaMetadataRepository = get()
        )
    }
}