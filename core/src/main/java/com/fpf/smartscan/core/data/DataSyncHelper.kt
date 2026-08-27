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

    suspend fun sync(
        context: Context,
        imageEmbedStores: List<FileEmbeddingStore>,
        videoEmbedStores: List<FileEmbeddingStore>,
        allowedImageDirs: List<Uri> = emptyList(),
        allowedVideoDirs: List<Uri> = emptyList(),
        mediaMetadataRepository: MediaMetadataRepository,
        clusterManager: ClusterManager,
        ){
        val clusterIdsFromImageSync = syncWithMediaStore(context,
            embedStores = imageEmbedStores,
            allowedDirs = allowedImageDirs,
            mediaMetadataRepository = mediaMetadataRepository,
            clusterManager = clusterManager,
            mediaType = MediaType.IMAGE
        )
        val clusterIdsFromVideoSync = syncWithMediaStore(context,
            embedStores = videoEmbedStores,
            allowedDirs = allowedVideoDirs,
            mediaMetadataRepository = mediaMetadataRepository,
            clusterManager = clusterManager,
            mediaType = MediaType.VIDEO
        )

        val clustersToSync = buildSet {
            addAll(clusterIdsFromImageSync)
            addAll(clusterIdsFromVideoSync)
        }
        clustersToSync.forEach { clusterManager.sync(it) }
    }

    private suspend fun syncWithMediaStore(
        context: Context,
        embedStores: List<FileEmbeddingStore>,
        allowedDirs: List<Uri> = emptyList(),
        mediaMetadataRepository: MediaMetadataRepository,
        clusterManager: ClusterManager,
        mediaType: MediaType,
    ): List<Long> {
        try {
            val existingIdsFromMetadata = mediaMetadataRepository.getIds(mediaType)
            if (existingIdsFromMetadata.isEmpty()) return emptyList()

            val accessibleMediaIds = when (mediaType) {
                MediaType.IMAGE -> MediaStoreHelper.queryImageIds(context, allowedDirs).toSet()
                MediaType.VIDEO -> MediaStoreHelper.queryVideoIds(context, allowedDirs).toSet()
            }

            val mediaToPurge = existingIdsFromMetadata.filterNot { it in accessibleMediaIds }
            var clustersToSync: List<Long> = emptyList()
            if (mediaToPurge.isNotEmpty()) {
                // Must get clusters first because deleting media causes cascading of cluster crossrefs (will break if not)
                clustersToSync = clusterManager.getClustersMatchingMedia(mediaToPurge, mediaType).map{it.clusterId}
                removeStaleMedia(mediaToPurge, mediaType, embedStores, mediaMetadataRepository)
                Log.d(TAG, "${mediaType.name}: Removed ${mediaToPurge.size} stale items")
            }
            return clustersToSync
        }catch (e: Exception){
            Log.e(TAG, "Error syncing with MediaStore\n Type: ${mediaType.name}\nDetails: $e")
            return emptyList()
        }
    }
}