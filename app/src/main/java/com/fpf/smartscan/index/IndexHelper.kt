package com.fpf.smartscan.index

import android.content.Context
import android.content.Intent
import com.fpf.smartscan.constants.EmbeddingStoresFilesQuant
import com.fpf.smartscan.data.clusters.ClusterCrossRefRepository
import com.fpf.smartscan.data.clusters.ClusterMetadataRepository
import com.fpf.smartscan.media.MediaType
import com.fpf.smartscan.services.MediaIndexForegroundService
import com.fpf.smartscan.utils.isServiceRunning
import com.fpf.smartscansdk.core.embeddings.FileEmbeddingStore
import java.io.File
import kotlin.collections.map


fun startIndexing(context: Context, mediaTypes: List<MediaType>, indexJob: IndexJobType = IndexJobType.LOCAL) {
    Intent(context.applicationContext, MediaIndexForegroundService::class.java)
        .putStringArrayListExtra(
            MediaIndexForegroundService.EXTRA_MEDIA_TYPES,
            ArrayList(mediaTypes.map { it.name })
        )
        .putExtra(MediaIndexForegroundService.EXTRA_INDEX_JOB, indexJob.name)
        .also { intent -> context.applicationContext.startForegroundService(intent) }
}

fun refreshIndex(context: Context, mediaTypes: List<MediaType>, indexJob: IndexJobType = IndexJobType.LOCAL) {
    val running = isServiceRunning(context.applicationContext, MediaIndexForegroundService::class.java)
    if(running){
        context.applicationContext.stopService(Intent(context.applicationContext, MediaIndexForegroundService::class.java))
    }
    startIndexing(context.applicationContext, mediaTypes, indexJob)
}

suspend fun rebuildIndex(context: Context, mediaEmbeddingStores: List<Pair<MediaType, FileEmbeddingStore>>, clusterCrossRefRepository: ClusterCrossRefRepository, clusterMetadataRepository: ClusterMetadataRepository) {
    mediaEmbeddingStores.forEach { typeToStore ->
        when(typeToStore.first){
            MediaType.IMAGE -> {
                typeToStore.second.clear()
                File(context.filesDir, EmbeddingStoresFilesQuant.IMAGE).delete()
            }
            MediaType.VIDEO -> {
                typeToStore.second.clear()
                File(context.filesDir, EmbeddingStoresFilesQuant.VIDEO).delete()
            }
        }
    }
    File(context.filesDir, EmbeddingStoresFilesQuant.CLUSTER).delete()
    clusterCrossRefRepository.clear()
    clusterMetadataRepository.clear()
    refreshIndex(context.applicationContext, mediaEmbeddingStores.map{it.first})
}