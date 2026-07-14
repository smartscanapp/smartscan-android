package com.fpf.smartscan

import android.app.Application
import android.content.Context
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.application
import androidx.lifecycle.viewModelScope
import com.fpf.smartscan.constants.EmbeddingStoresFiles
import com.fpf.smartscan.constants.PrefsKeys
import com.fpf.smartscan.constants.PrefsNames
import com.fpf.smartscan.data.DataSyncHelper
import com.fpf.smartscan.data.MediaDatabase
import com.fpf.smartscan.data.ModelRepository
import com.fpf.smartscan.data.clusters.ClusterCrossRefRepository
import com.fpf.smartscan.data.clusters.ClusterMetadataRepository
import com.fpf.smartscan.data.metadata.MediaMetadataRepository
import com.fpf.smartscan.index.CloudImageIndexListener
import com.fpf.smartscan.index.ImageIndexListener
import com.fpf.smartscan.index.IndexJobType
import com.fpf.smartscan.index.VideoIndexListener
import com.fpf.smartscan.index.rebuildIndex
import com.fpf.smartscan.index.refreshIndex
import com.fpf.smartscan.media.MediaType
import com.fpf.smartscan.settings.loadSettings
import com.fpf.smartscan.ui.permissions.StorageAccess
import com.fpf.smartscan.ui.permissions.getStorageAccess
import com.fpf.smartscan.utils.isWorkScheduled
import com.fpf.smartscan.workers.IndexWorker
import com.fpf.smartscansdk.core.embeddings.FileEmbeddingStore
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
    private val imageStore: FileEmbeddingStore,
    private val videoStore: FileEmbeddingStore,
    private val clusterStore: FileEmbeddingStore,
    private val clusterCrossRefRepository: ClusterCrossRefRepository,
    private val clusterMetadataRepository: ClusterMetadataRepository,
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
    } catch (e: Exception) {
        null
    }

    private val _isUpdatePopUpVisible = MutableStateFlow(!hasShownUpdatePopUp)
    val  isUpdatePopUpVisible: StateFlow<Boolean> = _isUpdatePopUpVisible

    val hasShownUpdatePopUp: Boolean
        get() = sharedPrefs.getString(PrefsKeys.UPDATES, null) == versionName

    fun closeUpdatePopUp(){
        _isUpdatePopUpVisible.value = false
        sharedPrefs.edit { putString(PrefsKeys.UPDATES, versionName.toString()) }
    }

    fun getUpdates(): List<String> {
        return listOf(
            application.getString(R.string.update_quantized_embeddings),
            application.getString(R.string.update_tagging),
            application.getString(R.string.update_strictness),
            application.getString(R.string.update_fixed_mediastore_collision_bug),
            application.getString(R.string.update_backups),
            )
    }

    fun prepareApp(onAppReady: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val appSettings = loadSettings(sharedPrefs)

            DataSyncHelper.quantEmbedStoresIfNeeded(
                mapOf(
                    File(application.filesDir, EmbeddingStoresFiles.IMAGE) to imageStore,
                    File(application.filesDir, EmbeddingStoresFiles.VIDEO) to videoStore,
                    File(application.filesDir, EmbeddingStoresFiles.MEDIA_CLUSTER) to clusterStore
                )
            )
            // Always run on app start to handle media that may have been deleted from the device
            DataSyncHelper.sync(
                application, imageStore = imageStore,
                videoStore=videoStore,
                allowedImageDirs = appSettings.searchableImageDirectories.map{it.toUri()},
                allowedVideoDirs = appSettings.searchableVideoDirectories.map{it.toUri()},
                mediaMetadataRepository = MediaMetadataRepository(db.metadataDao())
            )

            if(!isWorkScheduled(context = application, workName = IndexWorker.TAG)) scheduleIndexWorker()

            _hasIndexedImages.update { imageStore.exists }
            _hasIndexedVideos.update { videoStore.exists }
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
                    MediaType.IMAGE -> it to imageStore
                    MediaType.VIDEO -> it to videoStore
                }
            }
            viewModelScope.launch {
                _runningMediaTypes.update { mediaTypes.toSet()}
                rebuildIndex(getApplication(), mediaTypeToEmbedStore, clusterCrossRefRepository, clusterMetadataRepository)
            }
        }
    }

    fun onIndexingFinished(mediaType: MediaType) {
        when(mediaType){
            MediaType.IMAGE -> _hasIndexedImages.value = imageStore.exists
            MediaType.VIDEO -> _hasIndexedVideos.value = videoStore.exists
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
        if (!imageStore.exists && !videoStore.exists) return
        // Delay is required to prevent race condition issues on first index
        IndexWorker.scheduleWorker(getApplication(), Pair(1L, TimeUnit.DAYS), Pair(1L, TimeUnit.DAYS))
    }
}