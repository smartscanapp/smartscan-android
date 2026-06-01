package com.fpf.smartscan

import android.app.Application
import android.content.Context
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.application
import androidx.lifecycle.viewModelScope
import com.fpf.smartscan.constants.PrefsKeys
import com.fpf.smartscan.constants.PrefsNames
import com.fpf.smartscan.data.DbManager
import com.fpf.smartscan.data.EmbedStoreSyncHelper
import com.fpf.smartscan.data.MediaDatabase
import com.fpf.smartscan.media.MediaType
import com.fpf.smartscan.services.refreshIndex
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
            application.getString(R.string.update_copy_multiple_collections_to_tag),
            application.getString(R.string.update_date_filters_search),
            application.getString(R.string.update_rebuild_index),
            application.getString(R.string.update_tag_query_search_fix),
        )
    }

    fun prepareApp(onAppReady: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val cachedDb = DbManager.checkCachedDb(application)
            val isRestoreRequired = cachedDb != null
            if (isRestoreRequired) {
                DbManager.restoreDbFromCache(application, cachedDb)
            }

            if (!hasSyncedDates) {
                EmbedStoreSyncHelper.syncStores(getApplication(), imageStore, videoStore)
            }

            val mediaSyncNeeded = !hasSyncedMediaMetadata && (imageStore.exists || videoStore.exists)
            if (mediaSyncNeeded) {
                DbManager.syncMediaMetadataFromEmbedStores(application, db, imageStore=imageStore, videoStore=videoStore)
            }

            val oldImageCachedDb = DbManager.checkOldCachedImageDb(application)
            val oldVideoCachedDb = DbManager.checkOldCachedVideoDb(application)
            val transferNeeded = oldImageCachedDb != null && oldVideoCachedDb != null
            if (transferNeeded) {
                DbManager.transferOldDbToNew(application, oldImageCachedDb, oldVideoCachedDb, db)
            }

            if(!isWorkScheduled(context = application, workName = IndexWorker.TAG)) scheduleIndexWorker()


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