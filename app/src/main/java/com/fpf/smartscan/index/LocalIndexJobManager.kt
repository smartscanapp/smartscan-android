package com.fpf.smartscan.index

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.util.Log
import androidx.core.net.toUri
import com.fpf.smartscan.R
import com.fpf.smartscan.data.clusters.ClusterCrossRefRepository
import com.fpf.smartscan.data.clusters.ClusterMetadataRepository
import com.fpf.smartscan.data.media.MediaMetadataRepository
import com.fpf.smartscan.media.MediaType
import com.fpf.smartscan.cluster.ClusterManager
import com.fpf.smartscan.errors.AppException
import com.fpf.smartscan.media.MediaMetadata
import com.fpf.smartscan.media.MediaStoreHelper
import com.fpf.smartscan.settings.loadSettings
import com.fpf.smartscan.utils.showNotification
import com.fpf.smartscansdk.core.embeddings.Embedding
import com.fpf.smartscansdk.core.embeddings.FileEmbeddingStore
import com.fpf.smartscansdk.core.embeddings.ImageEmbeddingProvider
import com.fpf.smartscansdk.core.indexers.ImageIndexer
import com.fpf.smartscansdk.core.indexers.VideoIndexer
import com.fpf.smartscansdk.core.processors.BatchProcessor
import com.fpf.smartscansdk.ml.embeddings.clip.ClipImageEmbedder.Companion.IMAGE_SIZE_X
import com.fpf.smartscansdk.ml.embeddings.clip.ClipImageEmbedder.Companion.IMAGE_SIZE_Y
import kotlinx.coroutines.CancellationException
import kotlin.collections.map

class LocalIndexJobManager(
    private val application: Application,
    private val sharedPrefs: SharedPreferences,
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
        private const val NOTIFICATION_ID = 100
    }

    suspend fun run(mediaTypes: List<MediaType>){
        try {
            val appSettings = loadSettings(sharedPrefs)
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
                            appSettings.searchableImageDirectories.map { it.toUri() })
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
                            appSettings.searchableVideoDirectories.map { it.toUri() })
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
        catch (e: AppException.ClusterException)  {
            Log.e(TAG, e.message, e)
            val title = application.getString(R.string.notif_title_index_error_service, "Media")
            val content = application.getString(R.string.notif_content_cluster_error_service)
            showNotification(application, title, content, NOTIFICATION_ID + 1)
        }
        catch (e: CancellationException) {
            Log.w(TAG, "Indexing job cancelled:", e)
        }
        catch (e: Exception) {
            Log.e(TAG, "Indexing failed:", e)
            val title = application.getString(R.string.notif_title_index_error_service, "Media")
            val content = application.getString(R.string.notif_content_index_error_service)
            showNotification(application, title, content, NOTIFICATION_ID + 1)
        } finally {
            imageEmbedder.closeSession()
        }
    }

    private suspend fun indexMedia(
        context: Context,
        mediaType: MediaType,
        store: FileEmbeddingStore,
        indexer: BatchProcessor<Long, Pair<Long, Embedding>>,
        metadataRepo: MediaMetadataRepository,
        allowedDirs: List<Uri> = emptyList()
    ){
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
}