package com.fpf.smartscan.core.index

import android.app.Application
import android.net.Uri
import com.fpf.smartscan.core.data.media.MediaMetadataRepository
import com.fpf.smartscan.core.media.MediaType
import com.fpf.smartscan.core.cluster.ClusterManager
import com.fpf.smartscan.core.errors.AppException
import com.fpf.smartscansdk.core.embeddings.FileEmbeddingStore
import com.fpf.smartscansdk.core.embeddings.ImageEmbeddingProvider
import com.fpf.smartscansdk.core.processors.Concurrency
import com.fpf.smartscansdk.core.processors.ConcurrencyController
import com.fpf.smartscansdk.core.processors.ProcessorResult
import com.fpf.smartscansdk.ml.embeddings.clip.ClipImageEmbedder.Companion.IMAGE_SIZE_X
import com.fpf.smartscansdk.ml.embeddings.clip.ClipImageEmbedder.Companion.IMAGE_SIZE_Y

class LocalIndexJobManager(
    private val application: Application,
    private val imageEmbedder: ImageEmbeddingProvider,
    private val imageEmbedStore: FileEmbeddingStore,
    private val videoEmbedStore: FileEmbeddingStore,
    private val mediaMetadataRepository: MediaMetadataRepository,
    private val clusterManager: ClusterManager,
    private val useListener: Boolean = true
) {
    companion object {
        private const val TAG = "LocalIndexJobManager"
    }

    suspend fun run(
        mediaTypes: List<MediaType>,
        allowedImageDirs: List<Uri>,
        allowedVideoDirs: List<Uri>,
        onResult: (suspend (ProcessorResult, MediaType) -> Unit )? = null
    ):  Map<MediaType, ProcessorResult>{
        try {
            if(!imageEmbedder.isInitialized()) imageEmbedder.initialize()

            val results = mutableMapOf<MediaType, ProcessorResult>()
            val concurrencyController = ConcurrencyController(application)
            val concurrency = Concurrency.Dynamic{
                concurrencyController.calculateConcurrency()
            }

            mediaTypes.forEach { mediaType ->
                when (mediaType) {
                    MediaType.IMAGE -> {
                        val imageIndexer = ImageIndexer(
                            imageEmbedder,
                            context = application,
                            listener = if(useListener) ImageIndexListener else null,
                            store = imageEmbedStore,
                            mediaMetadataRepository = mediaMetadataRepository,
                            quantize = true,
                            concurrency=concurrency
                        )
                        val imagesResult = imageIndexer.index(allowedImageDirs)
                        results[mediaType] = imagesResult
                        onResult?.invoke(imagesResult, mediaType)
                    }

                    MediaType.VIDEO -> {
                        val videoIndexer = VideoIndexer(
                            imageEmbedder,
                            context = application,
                            listener = if(useListener) VideoIndexListener else null,
                            store = videoEmbedStore,
                            mediaMetadataRepository = mediaMetadataRepository,
                            quantize = true,
                            width = IMAGE_SIZE_X,
                            height = IMAGE_SIZE_Y,
                            concurrency=concurrency
                        )
                        val videosResult = videoIndexer.index( allowedVideoDirs)
                        results[mediaType] = videosResult
                        onResult?.invoke(videosResult, mediaType)
                    }
                }
            }

            try {
                val unclusteredItemIdsMap = mediaMetadataRepository.getUnclusteredItemIds()
                clusterManager.cluster(unclusteredItemIdsMap)
            } catch (e: Exception) {
                throw AppException.ClusterException(cause = e)
            }
            return results
        }
        finally {
            imageEmbedder.closeSession()
        }
    }
}