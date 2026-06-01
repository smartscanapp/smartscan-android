package com.fpf.smartscan

import android.app.Application
import android.content.Context
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.application
import androidx.lifecycle.viewModelScope
import com.fpf.smartscan.constants.PrefsKeys
import com.fpf.smartscan.constants.PrefsNames
import com.fpf.smartscan.data.DataSyncHelper
import com.fpf.smartscan.data.MediaDatabase
import com.fpf.smartscan.data.metadata.MediaMetadataRepository
import com.fpf.smartscan.media.MediaType
import com.fpf.smartscan.services.refreshIndex
import com.fpf.smartscan.settings.loadSettings
import com.fpf.smartscan.utils.isWorkScheduled
import com.fpf.smartscan.workers.IndexWorker
import com.fpf.smartscansdk.core.embeddings.FileEmbeddingStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.concurrent.TimeUnit

class MainViewModel(
    application: Application,
    private val db: MediaDatabase,
    private val imageStore: FileEmbeddingStore,
    private val videoStore: FileEmbeddingStore,
    private val clusterStore: FileEmbeddingStore,
    private  val mediaMetadataRepository: MediaMetadataRepository
) : AndroidViewModel(application) {

    companion object {
    }
    private val sharedPrefs = application.getSharedPreferences(PrefsNames.APP_PREFS, Context.MODE_PRIVATE)
    private val hasSyncedDates by lazy { sharedPrefs.getBoolean(PrefsKeys.EMBED_STORE_DATE_SYNC_COMPLETE, false)}
    private val hasSyncedMediaMetadata by lazy { sharedPrefs.getBoolean(PrefsKeys.MEDIA_METADATA_SYNC_COMPLETE, false)}


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
            application.getString(R.string.update_hide_duplicates),
            application.getString(R.string.update_merge_auto_collections),
            application.getString(R.string.update_move_media_auto_collections),
            application.getString(R.string.update_mixed_media_collections),
            application.getString(R.string.update_select_all_search_collections),
            application.getString(R.string.update_ui_improvements_bug_fixes),
        )
    }

    fun prepareApp(onAppReady: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val cachedDb = DataSyncHelper.checkCachedDb(application)
            val isRestoreRequired = cachedDb != null
            if (isRestoreRequired) {
                DataSyncHelper.restoreDbFromCache(application, cachedDb)
            }

            if (!hasSyncedDates) {
                DataSyncHelper.syncEmbedStoreDates(getApplication(), imageStore, videoStore)
            }

            val mediaSyncNeeded = !hasSyncedMediaMetadata && (imageStore.exists || videoStore.exists)
            if (mediaSyncNeeded) {
                DataSyncHelper.syncMediaMetadataFromEmbedStores(application, db, imageStore=imageStore, videoStore=videoStore)
            }

            val oldImageCachedDb = DataSyncHelper.checkOldCachedImageDb(application)
            val oldVideoCachedDb = DataSyncHelper.checkOldCachedVideoDb(application)
            val transferNeeded = oldImageCachedDb != null && oldVideoCachedDb != null
            if (transferNeeded) {
                DataSyncHelper.transferOldDbToNew(application, oldImageCachedDb, oldVideoCachedDb, db)
            }

            if(!isWorkScheduled(context = application, workName = IndexWorker.TAG)) scheduleIndexWorker()

            val appSettings = loadSettings(sharedPrefs)

            // Always run on app start to handle media that may have been deleted from the device
            // May switch to ContentObserver
            DataSyncHelper.syncWithMediaStore(
                application, imageStore = imageStore,
                videoStore=videoStore,
                allowedImageDirs = appSettings.searchableImageDirectories.map{it.toUri()},
                allowedVideoDirs = appSettings.searchableVideoDirectories.map{it.toUri()},
                mediaMetadataRepository = mediaMetadataRepository
            )

            val hasIndexedImagesButNotClustered = imageStore.exists && !clusterStore.exists
            val hasIndexedVideosButNotClustered =  videoStore.exists && !clusterStore.exists
            if(hasIndexedVideosButNotClustered && hasIndexedImagesButNotClustered){
                refreshIndex(getApplication(), MediaType.entries)
            }else{
                when{
                    hasIndexedVideosButNotClustered -> refreshIndex(getApplication(), mediaTypes = listOf(MediaType.VIDEO))
                    hasIndexedImagesButNotClustered ->  refreshIndex(getApplication(), mediaTypes = listOf(MediaType.IMAGE))
                }
            }

            onAppReady()
        }
    }

    private fun scheduleIndexWorker(){
        if (!imageStore.exists && !videoStore.exists) return
        // Delay is required to prevent race condition issues on first index
        IndexWorker.scheduleWorker(getApplication(), Pair(1L, TimeUnit.DAYS), Pair(1L, TimeUnit.DAYS))
    }

    override fun onCleared() {
        runBlocking {
            imageStore.save()
            videoStore.save()
        }
        super.onCleared()
    }

}