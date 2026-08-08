package com.fpf.smartscan.core.index

import android.app.Application
import android.content.Context
import android.net.Uri
import com.fpf.smartscan.core.data.clusters.ClusterCrossRefRepository
import com.fpf.smartscan.core.data.clusters.ClusterMetadataRepository
import com.fpf.smartscan.core.data.media.MediaMetadataRepository
import com.fpf.smartscan.core.media.MediaType
import com.fpf.smartscan.core.cluster.ClusterManager
import com.fpf.smartscan.core.errors.AppException
import com.fpf.smartscan.core.media.MediaMetadata
import com.fpf.smartscan.core.media.MediaStoreHelper
import com.fpf.smartscansdk.core.embeddings.Embedding
import com.fpf.smartscansdk.core.embeddings.FileEmbeddingStore
import com.fpf.smartscansdk.core.embeddings.ImageEmbeddingProvider
import com.fpf.smartscansdk.core.processors.BatchProcessor
import com.fpf.smartscansdk.ml.embeddings.clip.ClipImageEmbedder.Companion.IMAGE_SIZE_X
import com.fpf.smartscansdk.ml.embeddings.clip.ClipImageEmbedder.Companion.IMAGE_SIZE_Y
import kotlin.collections.map

class LocalIndexJobManager(
    private val application: Application,
    private val imageEmbedder: ImageEmbeddingProvider,
    private val imageEmbedStore: FileEmbeddingStore,
    private val videoEmbedStore: FileEmbeddingStore,
    private val clusterEmbedStore: FileEmbeddingStore,
    private val mediaMetadataRepository: MediaMetadataRepository,
    private val clusterMetadataRepository: ClusterMetadataRepository,
    private val clusterCrossRefRepository: ClusterCrossRefRepository,
    private val useListener: Boolean = true
) {
    companion object {
        private const val TAG = "LocalIndexJobManager"
    }

    suspend fun run(mediaTypes: List<MediaType>, allowedImageDirs: List<Uri>, allowedVideoDirs: List<Uri>){
        try {
            if(!imageEmbedder.isInitialized()) imageEmbedder.initialize()

            val clusterManager = ClusterManager(
                clusterEmbedStore = clusterEmbedStore,
                imageEmbedStore = imageEmbedStore,
                videoEmbedStore = videoEmbedStore,
                clusterCrossRefRepository = clusterCrossRefRepository,
                clusterMetadataRepository = clusterMetadataRepository,
            )

            mediaTypes.forEach { mediaType ->
                when (mediaType) {
                    MediaType.IMAGE -> {
                        val imageIndexer = ImageIndexer(
                            imageEmbedder,
                            context = application,
                            listener = if(useListener) ImageIndexListener else null,
                            store = imageEmbedStore,
                            quantize = true
                        )
                        indexMedia(
                            application,
                            MediaType.IMAGE,
                            imageEmbedStore,
                            imageIndexer,
                            mediaMetadataRepository,
                            allowedImageDirs
                        )
                    }

                    MediaType.VIDEO -> {
                        val videoIndexer = VideoIndexer(
                            imageEmbedder,
                            context = application,
                            listener = if(useListener) VideoIndexListener else null,
                            store = videoEmbedStore,
                            quantize = true,
                            width = IMAGE_SIZE_X,
                            height = IMAGE_SIZE_Y
                        )
                        indexMedia(
                            application,
                            MediaType.VIDEO,
                            videoEmbedStore,
                            videoIndexer,
                            mediaMetadataRepository,
                            allowedVideoDirs
                        )
                    }
                }
            }

            try {
                val unclusteredItemIdsMap = mediaMetadataRepository.getUnclusteredItemIds()
                clusterManager.cluster(unclusteredItemIdsMap)
            } catch (e: Exception) {
                throw AppException.ClusterException(cause = e)
            }

        }
        finally {
            imageEmbedder.closeSession()
        }
    }

    private suspend fun indexMedia(
        context: Context,
        mediaType: MediaType,
        store: FileEmbeddingStore,
        indexer: BatchProcessor<MediaMetadata, Pair<MediaMetadata, Embedding>>,
        metadataRepo: MediaMetadataRepository,
        allowedDirs: List<Uri> = emptyList()
    ){
        val idToDateMap = when(mediaType){
            MediaType.IMAGE -> MediaStoreHelper.queryImageIdDateMap(context, allowedDirs)
            MediaType.VIDEO ->  MediaStoreHelper.queryVideoIdDateMap(context, allowedDirs)
        }
        val existingMediaIdsInEmbedStore =( if(store.exists) store.get() else emptyList()).map{it.id}.toSet()
        val existingMediaMap = metadataRepo.get(mediaType).associateBy { it.id }
        val newMediaIds = idToDateMap.keys.filterNot { existingMediaMap.containsKey(it) && existingMediaIdsInEmbedStore.contains(it) }
        val newMedia = newMediaIds.mapNotNull{
            val date = idToDateMap[it]?: return@mapNotNull null
            MediaMetadata(it, mediaType, date)
        }
        metadataRepo.insert(newMedia)
        indexer.run(newMedia)
    }
}