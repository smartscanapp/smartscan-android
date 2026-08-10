package com.fpf.smartscan.core.data

import android.content.Context
import android.net.Uri
import android.util.Log
import com.fpf.smartscan.core.cluster.ClusterManager
import com.fpf.smartscan.core.data.media.MediaMetadataRepository
import com.fpf.smartscan.core.media.MediaStoreHelper
import com.fpf.smartscan.core.media.MediaType
import com.fpf.smartscan.core.media.removeStaleMedia
import com.fpf.smartscansdk.core.embeddings.FileEmbeddingStore
import com.fpf.smartscansdk.core.embeddings.toQInt8Embed
import java.io.File

object DataSyncHelper {
    const val TAG = "DataSyncHelper"
    private const val EMBED_DIM: Int = 512

    suspend fun quantEmbedStoresIfNeeded(oldFileToQuantStoreMap: Map<File, FileEmbeddingStore>){
        oldFileToQuantStoreMap.entries.forEach {
            if (!it.key.exists()) return@forEach
            quantizeEmbedStore(it.key, it.value)
        }
    }

    suspend fun sync(
        context: Context,
        imageEmbedStores: List<FileEmbeddingStore>,
        videoEmbedStores: List<FileEmbeddingStore>,
        allowedImageDirs: List<Uri> = emptyList(),
        allowedVideoDirs: List<Uri> = emptyList(),
        mediaMetadataRepository: MediaMetadataRepository,
        clusterManager: ClusterManager,
        ){
        val purgedImageIds = syncWithMediaStore(context,
            embedStores = imageEmbedStores,
            allowedDirs = allowedImageDirs,
            mediaMetadataRepository = mediaMetadataRepository,
            mediaType = MediaType.IMAGE
        )
        val purgedVideoIds = syncWithMediaStore(context,
            embedStores = videoEmbedStores,
            allowedDirs = allowedVideoDirs,
            mediaMetadataRepository = mediaMetadataRepository,
            mediaType = MediaType.VIDEO
        )

        val clustersToSync = buildSet {
            purgedImageIds.forEach { mediaId ->
                val clusters = clusterManager.getClustersMatchingMedia(mediaId, MediaType.IMAGE)
                addAll(clusters.map{it.clusterId})
            }
            purgedVideoIds.forEach { mediaId ->
                val clusters = clusterManager.getClustersMatchingMedia(mediaId, MediaType.VIDEO)
                addAll(clusters.map{it.clusterId})
            }
        }
        clustersToSync.forEach { clusterManager.sync(it) }
    }

    private suspend fun quantizeEmbedStore( oldEmbedStoreFile: File, quantStore: FileEmbeddingStore){
        val oldEmbedStore = FileEmbeddingStore(oldEmbedStoreFile, EMBED_DIM)
        val embeds = oldEmbedStore.get().map { it.copy(embedding = it.embedding.toQInt8Embed()) }
        quantStore.add(embeds)
        oldEmbedStore.clear()
        oldEmbedStoreFile.delete()
        Log.d(TAG, "Successfully added quantized embeddings from: ${oldEmbedStoreFile.name}")
    }


    private suspend fun syncWithMediaStore(
        context: Context,
        embedStores: List<FileEmbeddingStore>,
        allowedDirs: List<Uri> = emptyList(),
        mediaMetadataRepository: MediaMetadataRepository,
        mediaType: MediaType
    ): List<Long> {
        try {
            val existingIdsFromMetadata = mediaMetadataRepository.getIds(mediaType)
            if (existingIdsFromMetadata.isEmpty()) return emptyList()

            val accessibleMediaIds = when (mediaType) {
                MediaType.IMAGE -> MediaStoreHelper.queryImageIds(context, allowedDirs).toSet()
                MediaType.VIDEO -> MediaStoreHelper.queryVideoIds(context, allowedDirs).toSet()
            }

            val mediaToPurge = existingIdsFromMetadata.filterNot { it in accessibleMediaIds }
            if (mediaToPurge.isNotEmpty()) {
                removeStaleMedia(mediaToPurge, mediaType, embedStores, mediaMetadataRepository)
                Log.d(TAG, "${mediaType.name}: Removed ${mediaToPurge.size} stale items")
            }
            return mediaToPurge
        }catch (e: Exception){
            Log.e(TAG, "Error syncing with MediaStore\n Type: ${mediaType.name}\nDetails: $e")
            return emptyList()
        }
    }
}