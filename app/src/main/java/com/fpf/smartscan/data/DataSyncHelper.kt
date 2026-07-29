package com.fpf.smartscan.data

import android.content.Context
import android.net.Uri
import android.util.Log
import com.fpf.smartscan.data.media.MediaMetadataRepository
import com.fpf.smartscan.media.MediaStoreHelper
import com.fpf.smartscan.media.MediaType
import com.fpf.smartscan.media.removeStaleMedia
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
        imageStore: FileEmbeddingStore,
        videoStore: FileEmbeddingStore,
        allowedImageDirs: List<Uri> = emptyList(),
        allowedVideoDirs: List<Uri> = emptyList(),
        mediaMetadataRepository: MediaMetadataRepository
        ){
        syncWithMediaStore(context,
            store = imageStore,
            allowedDirs = allowedImageDirs,
            mediaMetadataRepository = mediaMetadataRepository,
            mediaType = MediaType.IMAGE
        )
        syncWithMediaStore(context,
            store = videoStore,
            allowedDirs = allowedVideoDirs,
            mediaMetadataRepository = mediaMetadataRepository,
            mediaType = MediaType.VIDEO
        )
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
        store: FileEmbeddingStore,
        allowedDirs: List<Uri> = emptyList(),
        mediaMetadataRepository: MediaMetadataRepository,
        mediaType: MediaType
    ) {
        try {
            val existingIdsFromMetadata = mediaMetadataRepository.getIdsByType(mediaType)
            if (existingIdsFromMetadata.isEmpty()) return

            val accessibleMediaIds = when (mediaType) {
                MediaType.IMAGE -> MediaStoreHelper.queryImageIds(context, allowedDirs).toSet()
                MediaType.VIDEO -> MediaStoreHelper.queryVideoIds(context, allowedDirs).toSet()
            }

            val mediaToPurge = existingIdsFromMetadata.filterNot { it in accessibleMediaIds }
            if (mediaToPurge.isNotEmpty()) {
                removeStaleMedia(mediaToPurge, mediaType, listOf(store), mediaMetadataRepository)
                Log.d(TAG, "${mediaType.name}: Removed ${mediaToPurge.size} stale items")
            }
        }catch (e: Exception){
            Log.e(TAG, "Error syncing with MediaStore\n Type: ${mediaType.name}\nDetails: $e")
        }
    }
}