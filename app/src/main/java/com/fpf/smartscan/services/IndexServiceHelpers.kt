package com.fpf.smartscan.services

import android.content.Context
import android.content.Intent
import com.fpf.smartscan.core.cluster.ClusterManager
import com.fpf.smartscan.core.embeds.EmbeddingStoresFilesQuant
import com.fpf.smartscan.core.index.IndexJobType
import com.fpf.smartscan.core.media.MediaType
import com.fpf.smartscan.utils.isServiceRunning
import com.fpf.smartscansdk.core.embeddings.FileEmbeddingStore
import java.io.File
import kotlin.collections.map


fun startIndexing(context: Context, mediaTypes: List<MediaType>, indexJob: IndexJobType = IndexJobType.LOCAL) {
    Intent(context.applicationContext, IndexService::class.java)
        .putStringArrayListExtra(
            IndexService.EXTRA_MEDIA_TYPES,
            ArrayList(mediaTypes.map { it.name })
        )
        .putExtra(IndexService.EXTRA_INDEX_JOB, indexJob.name)
        .also { intent -> context.applicationContext.startForegroundService(intent) }
}

fun refreshIndex(context: Context, mediaTypes: List<MediaType>, indexJob: IndexJobType = IndexJobType.LOCAL) {
    val running = isServiceRunning(context.applicationContext, IndexService::class.java)
    if(running){
        stopIndexing(context)
    }
    startIndexing(context.applicationContext, mediaTypes, indexJob)
}

suspend fun rebuildIndex(context: Context, mediaEmbeddingStores: List<Pair<MediaType, FileEmbeddingStore>>, clusterManager: ClusterManager) {
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
    clusterManager.deleteAllClusters(context)
    refreshIndex(context.applicationContext, mediaEmbeddingStores.map{it.first})
}

fun stopIndexing(context: Context){
    context.applicationContext.stopService(Intent(context.applicationContext, IndexService::class.java))

}