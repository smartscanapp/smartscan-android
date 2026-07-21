package com.fpf.smartscan.di

import org.koin.core.module.dsl.viewModel
import com.fpf.smartscan.MainViewModel
import com.fpf.smartscan.ui.screens.collections.CollectionItemsViewModel
import com.fpf.smartscan.ui.screens.collections.CollectionsViewModel
import com.fpf.smartscan.ui.screens.concepts.ConceptItemsViewModel
import com.fpf.smartscan.ui.screens.concepts.ConceptsViewModel
import com.fpf.smartscan.ui.screens.search.SearchViewModel
import com.fpf.smartscan.ui.screens.settings.SettingsViewModel
import com.fpf.smartscan.ui.shared.MediaViewModel
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
            clusterMetadataRepository = get(),
            modelRepository = get()
        )
    }
    viewModel {
        SearchViewModel(
            application = get(),
            imageEmbedStore = get(IMAGE_EMBED_STORE),
            videoEmbedStore = get(VIDEO_EMBED_STORE),
            clusterEmbedStore = get(CLUSTER_EMBED_STORE),
            mediaMetadataRepository = get(),
            tagManager = get(),
            clusterCrossRefRepository = get(),
            modelRepository = get()
        )
    }
    viewModel {
        CollectionItemsViewModel(
            application = get(),
            imageEmbedStore = get(IMAGE_EMBED_STORE),
            videoEmbedStore = get(VIDEO_EMBED_STORE),
            mediaMetadataRepository = get(),
            clusterManager = get(),
            tagManager = get()
        )
    }

    viewModel {
        CollectionsViewModel(
            application = get(),
            clusterManager = get(),
            tagManager = get()
        )
    }
    viewModel {
        ConceptsViewModel(
            application = get(),
            tagRepository = get(),
            clusterMetadataRepository = get(),
            conceptManager = get(),
            modelRepository = get(),
            )
    }
    viewModel {
        ConceptItemsViewModel(
            application = get(),
            mediaMetadataRepository = get()
        )
    }
    viewModel {
        SettingsViewModel(
            application = get(),
            modelRepository = get()
        )
    }

    viewModel {
        MediaViewModel(
            mediaJobManager = get(),
            tagRepository = get(),
            clusterMetadataRepository = get()
        )
    }
}