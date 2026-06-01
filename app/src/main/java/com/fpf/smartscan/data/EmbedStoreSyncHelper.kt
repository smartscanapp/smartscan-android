package com.fpf.smartscan.data

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.util.Log
import androidx.core.content.edit
import com.fpf.smartscan.constants.EmbeddingStoresFiles
import com.fpf.smartscan.constants.PrefsKeys
import com.fpf.smartscan.constants.PrefsNames
import com.fpf.smartscan.media.getImageToDateMap
import com.fpf.smartscan.media.getVideoToDateMap
import com.fpf.smartscansdk.core.embeddings.FileEmbeddingStore
import java.io.File
import kotlin.collections.map
import kotlin.collections.mapNotNull

object EmbedStoreSyncHelper {
    private const val TAG = "EmbedStoreSyncHelper"
    private const val EMBED_DIM: Int = 512

    suspend fun syncStores(
        context: Context,
        imageStore: FileEmbeddingStore,
        videoStore: FileEmbeddingStore
    ) {
        val sharedPrefs = context.applicationContext.getSharedPreferences(PrefsNames.APP_PREFS, MODE_PRIVATE)

        val syncedImages = syncStoreDates(
            context=context,
            store = imageStore,
            tempFileName = "${EmbeddingStoresFiles.IMAGE}.tmp",
            finalFileName = EmbeddingStoresFiles.IMAGE,
            idToDate = { ids -> getImageToDateMap(context.applicationContext, ids) }
        )

        val syncedVideos = syncStoreDates(
            context=context,
            store = videoStore,
            tempFileName = "${EmbeddingStoresFiles.VIDEO}.tmp",
            finalFileName = EmbeddingStoresFiles.VIDEO,
            idToDate = { ids -> getVideoToDateMap(context.applicationContext, ids) }
        )

        if(syncedVideos || syncedImages){
            sharedPrefs.edit {
                putBoolean(PrefsKeys.EMBED_STORE_DATE_SYNC_COMPLETE, true)
            }
            Log.d(TAG, "Sync complete successfully")
        }
    }

    private suspend fun syncStoreDates(
        context: Context,
        store: FileEmbeddingStore,
        tempFileName: String,
        finalFileName: String,
        idToDate: suspend (List<Long>) -> Map<Long, Long>,
    ): Boolean {
        if (!store.exists) return false
        val embeds = store.get()
        val dateMap = idToDate(embeds.map { it.id })
        val updated = embeds.mapNotNull {
            val date = dateMap[it.id] ?: return@mapNotNull null
            it.copy(date = date)
        }

        val tempFile = File(context.applicationContext.cacheDir, tempFileName)
        val tempStore = FileEmbeddingStore(tempFile, EMBED_DIM)
        tempStore.add(updated)

        val finalFile = File(context.applicationContext.filesDir, finalFileName)
        if (finalFile.exists()) finalFile.delete()
        tempFile.renameTo(finalFile)
        return true
    }

}
