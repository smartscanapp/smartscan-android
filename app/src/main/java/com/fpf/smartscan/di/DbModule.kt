package com.fpf.smartscan.di

import com.fpf.smartscan.data.MediaDatabase
import com.fpf.smartscan.data.clusters.ClusterCrossRefRepository
import com.fpf.smartscan.data.clusters.ClusterMetadataRepository
import com.fpf.smartscan.data.concepts.ConceptCrossRefRepository
import com.fpf.smartscan.data.concepts.ConceptRepository
import com.fpf.smartscan.data.media.MediaMetadataRepository
import com.fpf.smartscan.data.tags.TagCrossRefRepository
import com.fpf.smartscan.data.tags.TagRepository
import org.koin.android.ext.koin.androidApplication
import org.koin.dsl.module

val dbModule = module {

    single {
        MediaDatabase.getDatabase(androidApplication())
    }

    single { get<MediaDatabase>().metadataDao() }
    single { get<MediaDatabase>().tagDao() }
    single { get<MediaDatabase>().tagCrossRefDao() }
    single { get<MediaDatabase>().clusterCrossRefDao() }
    single { get<MediaDatabase>().clusterMetadataDao() }
    single { get<MediaDatabase>().conceptDao() }
    single { get<MediaDatabase>().conceptCrossRefDao() }

    single { MediaMetadataRepository(get()) }
    single { TagRepository(get()) }
    single { TagCrossRefRepository(get()) }
    single { ClusterCrossRefRepository(get()) }
    single { ClusterMetadataRepository(get()) }
    single { ConceptCrossRefRepository(get()) }
    single { ConceptRepository(get()) }

}