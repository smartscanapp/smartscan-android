package com.fpf.smartscan

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.application
import androidx.lifecycle.viewModelScope
import com.fpf.smartscan.cloud.index.CloudImageIndexListener
import com.fpf.smartscan.core.embeds.EmbeddingStoresFiles
import com.fpf.smartscan.constants.PrefsKeys
import com.fpf.smartscan.constants.PrefsNames
import com.fpf.smartscan.core.cluster.ClusterManager
import com.fpf.smartscan.core.data.DataSyncHelper
import com.fpf.smartscan.core.data.MediaDatabase
import com.fpf.smartscan.core.models.ModelRepository
import com.fpf.smartscan.core.data.clusters.ClusterCrossRefRepository
import com.fpf.smartscan.core.data.clusters.ClusterMetadataRepository
import com.fpf.smartscan.core.data.media.MediaMetadataRepository
import com.fpf.smartscan.core.index.ImageIndexListener
import com.fpf.smartscan.core.index.IndexJobType
import com.fpf.smartscan.core.index.VideoIndexListener
import com.fpf.smartscan.services.rebuildIndex
import com.fpf.smartscan.services.refreshIndex
import com.fpf.smartscan.core.media.MediaType
import com.fpf.smartscan.settings.loadSettings
import com.fpf.smartscan.ui.permissions.StorageAccess
import com.fpf.smartscan.ui.permissions.getStorageAccess
import com.fpf.smartscan.utils.getTimeInMinutesAndSeconds
import com.fpf.smartscan.workers.IndexWorker
import com.fpf.smartscan.workers.isWorkScheduled
import com.fpf.smartscansdk.core.embeddings.FileEmbeddingStore
import com.fpf.smartscansdk.ml.models.ModelName
import com.fpf.smartscansdk.ml.models.ModelRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.TimeUnit

class MainViewModel(
    application: Application,
    private val db: MediaDatabase,
    private val imageEmbedStore: FileEmbeddingStore,
    private val videoEmbedStore: FileEmbeddingStore,
    private val clusterEmbedStore: FileEmbeddingStore,
    private val imageConceptEmbedStore: FileEmbeddingStore,
    private val videoConceptEmbedStore: FileEmbeddingStore,
    private val clusterManager: ClusterManager,
    private val modelRepository: ModelRepository
) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "MainViewModel"
    }

    private val sharedPrefs = application.getSharedPreferences(PrefsNames.APP_PREFS, Context.MODE_PRIVATE)

    // Global indexing state
    val imageIndexProgress = ImageIndexListener.progress
    val imageIndexStatus = ImageIndexListener.indexingStatus
    val videoIndexProgress = VideoIndexListener.progress
    val videoIndexStatus = VideoIndexListener.indexingStatus

    val modelDownloadProgress = modelRepository.modelDownloadProgress
    val modelDownloadStatus = modelRepository.modelDownloadStatus
    val installedModels = modelRepository.installedModels

    val cloudImageIndexProgress = CloudImageIndexListener.progress
    val cloudImageIndexStatus = CloudImageIndexListener.indexingStatus

    private val _hasIndexedImages = MutableStateFlow<Boolean?>(null)
    private val _hasIndexedVideos = MutableStateFlow<Boolean?>(null)
    val hasIndexedImages: StateFlow<Boolean?> = _hasIndexedImages
    val hasIndexedVideos: StateFlow<Boolean?> = _hasIndexedVideos
    private val _runningMediaTypes = MutableStateFlow<Set<MediaType>>(setOf())
    val runningMediaTypes: StateFlow<Set<MediaType>> = _runningMediaTypes

    val versionName: String? = try {
        val packageInfo = application.packageManager.getPackageInfo(application.packageName, 0)
        packageInfo.versionName
    } catch (_: Exception) {
        null
    }

    val storedVersion: String?
        get() = sharedPrefs.getString(PrefsKeys.UPDATES, null)

    private val _isUpdatePopUpVisible = MutableStateFlow(storedVersion != versionName && storedVersion != null)
    val  isUpdatePopUpVisible: StateFlow<Boolean> = _isUpdatePopUpVisible

    fun closeUpdatePopUp(){
        _isUpdatePopUpVisible.value = false
        sharedPrefs.edit { putString(PrefsKeys.UPDATES, versionName.toString()) }
    }

    fun setVersion() = sharedPrefs.edit { putString(PrefsKeys.UPDATES, versionName.toString()) }

    fun getUpdates(): List<String> {
        return listOf(
            application.getString(R.string.update_quantized_embeddings),
            application.getString(R.string.update_tagging),
            application.getString(R.string.update_strictness),
            application.getString(R.string.update_fixed_mediastore_collision_bug),
            application.getString(R.string.update_backups)
        )
    }

    fun prepareApp(onAppReady: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val appSettings = loadSettings(sharedPrefs)

            DataSyncHelper.quantEmbedStoresIfNeeded(
                mapOf(
                    File(application.filesDir, EmbeddingStoresFiles.IMAGE) to imageEmbedStore,
                    File(application.filesDir, EmbeddingStoresFiles.VIDEO) to videoEmbedStore,
                    File(application.filesDir, EmbeddingStoresFiles.MEDIA_CLUSTER) to clusterEmbedStore
                )
            )
            // Always run on app start to handle media that may have been deleted from the device
            DataSyncHelper.sync(
                application,
                imageEmbedStores = listOf(imageEmbedStore, imageConceptEmbedStore),
                videoEmbedStores = listOf(videoEmbedStore, videoConceptEmbedStore),
                allowedImageDirs = appSettings.searchableImageDirectories.map{it.toUri()},
                allowedVideoDirs = appSettings.searchableVideoDirectories.map{it.toUri()},
                mediaMetadataRepository = MediaMetadataRepository(db.metadataDao()),
                clusterManager = clusterManager,
            )

            // One time sync if required to remove stale clusters embeds which could exist
            // because the current mechanism which syncs clusters after media purging was not in place in older versions
            val hasSyncedClustersWithRoom: Boolean = sharedPrefs.getBoolean(PrefsKeys.HAS_SYNCED_CLUSTERS, false)
            if(!hasSyncedClustersWithRoom){
                clusterManager.syncEmbedsWithRoom()
                sharedPrefs.edit { putBoolean(PrefsKeys.HAS_SYNCED_CLUSTERS, true) }
            }

            if(!isWorkScheduled(context = application, workName = IndexWorker.TAG)) scheduleIndexWorker()

            _hasIndexedImages.update { imageEmbedStore.exists }
            _hasIndexedVideos.update { videoEmbedStore.exists }

            if(storedVersion == null) setVersion()

            onAppReady()
        }
    }

    fun refreshMediaIndex(mediaTypes: List<MediaType>){
        val storageAccess = getStorageAccess(getApplication())
        if (storageAccess != StorageAccess.Denied) {
            _runningMediaTypes.update { mediaTypes.toSet()}
            refreshIndex(getApplication(), mediaTypes)
        }
    }

    fun rebuildMediaIndex(mediaTypes: List<MediaType>){
        val storageAccess = getStorageAccess(getApplication())
        if (storageAccess != StorageAccess.Denied) {
            val mediaTypeToEmbedStore = mediaTypes.map{
                when(it) {
                    MediaType.IMAGE -> it to imageEmbedStore
                    MediaType.VIDEO -> it to videoEmbedStore
                }
            }
            viewModelScope.launch {
                _runningMediaTypes.update { mediaTypes.toSet()}
                rebuildIndex(getApplication(), mediaTypeToEmbedStore, clusterManager)
            }
        }
    }

    fun onIndexingFinished(mediaType: MediaType) {
        when(mediaType){
            MediaType.IMAGE -> _hasIndexedImages.value = imageEmbedStore.exists
            MediaType.VIDEO -> _hasIndexedVideos.value = videoEmbedStore.exists
        }
        resetIndexingState(mediaType)
        _runningMediaTypes.update { it - mediaType}
    }

    fun onConceptIndexingFinished(mediaType: MediaType) {
        resetConceptIndexingState(mediaType)
        _runningMediaTypes.update { it - mediaType}
    }

    fun startConceptIndexing(mediaTypes: List<MediaType>){
        val storageAccess = getStorageAccess(getApplication())
        if (storageAccess != StorageAccess.Denied) {
            _runningMediaTypes.update { mediaTypes.toSet()}
            refreshIndex(getApplication(), mediaTypes, IndexJobType.CLOUD)
        }
    }

    fun resetModelProgress() = modelRepository.reset()

    fun downloadModel() = modelRepository.downloadModel(ModelRegistry[ModelName.ALL_MINILM_L6_V2]!!)

    fun getIndexFailNotification(mediaType: MediaType): Pair<String, String>{
        val title = getApplication<Application>().getString(R.string.notif_title_index_error_service, mediaType.name.lowercase().replaceFirstChar { it.uppercase() })
        val content = getApplication<Application>().getString(R.string.notif_content_index_error_service)
        return Pair(title, content)
    }

    fun getIndexCompleteNotification(mediaType: MediaType): String?{
        val metrics = when(mediaType){
            MediaType.IMAGE -> ImageIndexListener.result.value
            MediaType.VIDEO -> VideoIndexListener.result.value
        }?: return null
        if(metrics.totalProcessed == 0) return null
        val (minutes, seconds) = getTimeInMinutesAndSeconds(metrics.timeElapsed)
        val notificationText = "Total ${mediaType.name.lowercase()}s indexed: ${metrics.totalProcessed}, Time: ${minutes}m ${seconds}s"
        return notificationText
    }

    private fun resetIndexingState(mediaType: MediaType){
        when(mediaType){
            MediaType.IMAGE -> ImageIndexListener.reset()
            MediaType.VIDEO -> VideoIndexListener.reset()
        }
    }

    private fun resetConceptIndexingState(mediaType: MediaType){
        when(mediaType){
            MediaType.IMAGE -> CloudImageIndexListener.reset()
            MediaType.VIDEO -> {}
        }
    }

    private fun scheduleIndexWorker(){
        if (!imageEmbedStore.exists && !videoEmbedStore.exists) return
        // Delay is required to prevent race condition issues on first index
        IndexWorker.scheduleWorker(getApplication(), Pair(1L, TimeUnit.DAYS), Pair(1L, TimeUnit.DAYS))
    }
}