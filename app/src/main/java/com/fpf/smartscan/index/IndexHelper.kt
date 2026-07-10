package com.fpf.smartscan.index

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.fpf.smartscan.constants.EmbeddingStoresFilesQuant
import com.fpf.smartscan.data.clusters.ClusterCrossRefRepository
import com.fpf.smartscan.data.clusters.ClusterMetadataRepository
import com.fpf.smartscan.data.metadata.MediaMetadataRepository
import com.fpf.smartscan.media.MediaMetadata
import com.fpf.smartscan.media.MediaStoreHelper
import com.fpf.smartscan.media.MediaType
import com.fpf.smartscan.services.MediaIndexForegroundService
import com.fpf.smartscan.utils.isServiceRunning
import com.fpf.smartscansdk.core.embeddings.Embedding
import com.fpf.smartscansdk.core.embeddings.FileEmbeddingStore
import com.fpf.smartscansdk.core.processors.BatchProcessor
import java.io.File
import kotlin.collections.map


fun startIndexing(context: Context, mediaTypes: List<MediaType>, indexJob: IndexJob = IndexJob.MAIN) {
    Intent(context.applicationContext, MediaIndexForegroundService::class.java)
        .putStringArrayListExtra(
            MediaIndexForegroundService.EXTRA_MEDIA_TYPES,
            ArrayList(mediaTypes.map { it.name })
        )
        .putExtra(MediaIndexForegroundService.EXTRA_INDEX_JOB, indexJob)
        .also { intent -> context.applicationContext.startForegroundService(intent) }
}

fun refreshIndex(context: Context, mediaTypes: List<MediaType>, indexJob: IndexJob = IndexJob.MAIN) {
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
suspend fun indexMedia(context: Context,  mediaType: MediaType, store: FileEmbeddingStore, indexer: BatchProcessor<Long, Pair<Long, Embedding>>, metadataRepo: MediaMetadataRepository, allowedDirs: List<Uri> = emptyList()){
    val idToDateMap = when(mediaType){
        MediaType.IMAGE -> MediaStoreHelper.queryImageIdDateMap(context, allowedDirs)
        MediaType.VIDEO ->  MediaStoreHelper.queryVideoIdDateMap(context, allowedDirs)
    }
    val existingMediaIdsInEmbedStore =( if(store.exists) store.get() else emptyList()).map{it.id}.toSet()
    val existingMediaMap = metadataRepo.getByType(mediaType).associateBy { it.id }
    val newMediaIds = idToDateMap.keys.filterNot { existingMediaMap.containsKey(it) && existingMediaIdsInEmbedStore.contains(it) }
    val newMedia = newMediaIds.mapNotNull{
        val date = idToDateMap[it]?: return@mapNotNull null
        MediaMetadata(it, mediaType, date)
    }
    metadataRepo.insert(newMedia)
    indexer.run(newMediaIds)
}

suspend fun indexMediaForConcepts(
    mediaType: MediaType,
    indexer: BatchProcessor<MediaMetadata, Pair<MediaMetadata, Embedding>>,
    metadataRepo: MediaMetadataRepository,
    allowedTags: List<Long>,
    allowedClusters: List<Long>,
){
    val mediaProcess = mutableSetOf<MediaMetadata>()
    when{
        allowedTags.isNotEmpty() -> {
            val existingMediaMatchingTags = metadataRepo.getByTagsWithoutDescription(allowedTags, mediaType)
            mediaProcess.addAll(existingMediaMatchingTags)
        }
        allowedClusters.isNotEmpty() -> {
            val existingMediaMatchingClusters = metadataRepo.getByClustersWithoutDescription(allowedClusters, mediaType)
            mediaProcess.addAll(existingMediaMatchingClusters)
        }
    }
    if(mediaProcess.isEmpty()) return
    indexer.run(mediaProcess.toList())
}