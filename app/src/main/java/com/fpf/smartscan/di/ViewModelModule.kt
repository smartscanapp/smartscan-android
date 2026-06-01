package com.fpf.smartscan.di

import org.koin.core.module.dsl.viewModel
import com.fpf.smartscan.MainViewModel
import com.fpf.smartscan.ui.screens.collections.CollectionItemsViewModel
import com.fpf.smartscan.ui.screens.collections.CollectionsViewModel
import com.fpf.smartscan.ui.screens.search.SearchViewModel
import org.koin.dsl.module

val viewModelModule = module {

    viewModel {
        MainViewModel(
            application = get(),
            db = get(),
            imageStore = get(IMAGE_STORE),
            videoStore = get(VIDEO_STORE),
            clusterStore = get(CLUSTER_STORE),
            )
    }
    viewModel {
        SearchViewModel(
            application = get(),
            imageStore = get(IMAGE_STORE),
            videoStore = get(VIDEO_STORE),
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
            imageStore = get(IMAGE_STORE),
            videoStore = get(VIDEO_STORE),
            mediaMetadataRepository = get(),
            tagRepository = get(),
            tagCrossRefRepository = get(),
            clusterCrossRefRepository = get(),
            clusterMetadataRepository = get(),
            clusterStore = get(CLUSTER_STORE),
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
            imageStore = get(IMAGE_STORE),
            videoStore = get(VIDEO_STORE),
            clusterStore = get(CLUSTER_STORE),
            )
    }
}