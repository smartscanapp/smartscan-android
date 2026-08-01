package com.fpf.smartscan.di

import com.fpf.smartscan.core.cluster.ClusterManager
import org.koin.dsl.module


val clusterModule = module {
    single {
        ClusterManager(
            clusterMetadataRepository = get(),
            clusterCrossRefRepository = get(),
            clusterEmbedStore = get(CLUSTER_EMBED_STORE),
            imageEmbedStore = get(IMAGE_EMBED_STORE),
            videoEmbedStore = get(VIDEO_EMBED_STORE)
        )
    }
}
