package com.fpf.smartscan.data

import android.app.Application
import android.content.Context.MODE_PRIVATE
import android.util.Log
import androidx.core.content.edit
import com.fpf.smartscan.constants.PrefsKeys
import com.fpf.smartscan.constants.PrefsNames
import com.fpf.smartscan.data.MediaDatabase.Companion.DB_NAME
import com.fpf.smartscan.data.MediaDatabase.Companion.OLD_DB_IMAGE_NAME
import com.fpf.smartscan.data.MediaDatabase.Companion.OLD_DB_VIDEO_NAME
import com.fpf.smartscan.data.metadata.MediaMetadata
import com.fpf.smartscan.data.metadata.MediaMetadataRepository
import com.fpf.smartscan.data.old.images.ImageTagCrossRefRepository
import com.fpf.smartscan.data.old.images.ImageTagDatabase
import com.fpf.smartscan.data.old.images.ImageTagRepository
import com.fpf.smartscan.data.tags.TagCrossRefRepository
import com.fpf.smartscan.data.tags.TagRepository
import com.fpf.smartscan.data.old.videos.VideoTagCrossRefRepository
import com.fpf.smartscan.data.old.videos.VideoTagDatabase
import com.fpf.smartscan.data.old.videos.VideoTagRepository
import com.fpf.smartscan.data.tags.Tag
import com.fpf.smartscan.data.tags.TagCrossRef
import com.fpf.smartscan.media.MediaType
import com.fpf.smartscan.media.queryImageIdDateMap
import com.fpf.smartscan.media.queryVideoIdDateMap
import com.fpf.smartscansdk.core.embeddings.FileEmbeddingStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File


object DbManager {
    const val TAG = "DbManager"

    fun checkCachedDb(application: Application): File?{
        val cachedDB = File(application.filesDir, DB_NAME)
        return if(cachedDB.exists()) cachedDB else null
    }

    fun checkOldCachedImageDb(application: Application): File?{
        val cachedDB = File(application.filesDir, OLD_DB_IMAGE_NAME)
        return if(cachedDB.exists()) cachedDB else null
    }

    fun checkOldCachedVideoDb(application: Application): File?{
        val cachedDB = File(application.filesDir, OLD_DB_VIDEO_NAME)
        return if(cachedDB.exists()) cachedDB else null
    }

    fun restoreDbFromCache(application: Application, cachedDbFile: File){
        if(!cachedDbFile.exists()) return
        val dbPath = application.getDatabasePath(DB_NAME)
        Log.d(TAG, "Database cache found, restoring...")
        cachedDbFile.copyTo(dbPath, overwrite = true)
        cachedDbFile.delete()
    }

    suspend fun transferOldDbToNew(application: Application, oldImageTagDbCachedFile: File, oldVideoTagDbCachedFile: File, newDb: MediaDatabase){
        val isTransferNeeded = oldImageTagDbCachedFile.exists() && oldVideoTagDbCachedFile.exists()
        if (!isTransferNeeded) return

        val oldImageTagDbPath = application.getDatabasePath(OLD_DB_IMAGE_NAME)
        val oldVideoTagDbPath = application.getDatabasePath(OLD_DB_VIDEO_NAME)

        Log.d(MediaDatabase.TAG, "Old DB detected, transferring...")

        oldImageTagDbCachedFile.copyTo(oldImageTagDbPath, overwrite = true)
        oldVideoTagDbCachedFile.copyTo(oldVideoTagDbPath, overwrite = true)

        transfer(
            application = application,
            newDb = newDb
        )
        oldImageTagDbCachedFile.delete()
        oldVideoTagDbCachedFile.delete()
        oldImageTagDbPath.delete()
        oldVideoTagDbPath.delete()
    }

    suspend fun syncMediaMetadataFromEmbedStores(
        application: Application,
        db: MediaDatabase,
        imageStore: FileEmbeddingStore,
        videoStore: FileEmbeddingStore
    ){
        val sharedPrefs = application.getSharedPreferences(PrefsNames.APP_PREFS, MODE_PRIVATE)
        val metadataRepo = MediaMetadataRepository(db.metadataDao())
        val storedImageIds = (if(imageStore.exists) imageStore.get() else emptyList()).map { it.id }.toSet()

        if(storedImageIds.isNotEmpty()) {
            val imageToDateMap = queryImageIdDateMap(application)
            val imageMetadataList = imageToDateMap.entries.mapNotNull{
                // Ensure embed store and media table are in sync
                // Filtered out items will be naturally added to index based on schedule
                if (it.key !in storedImageIds) return@mapNotNull null
                MediaMetadata(id=it.key, dateAdded = it.value, type = MediaType.IMAGE)
            }
            metadataRepo.insert(imageMetadataList)
            Log.d(TAG, "Image metadata sync complete. ${imageMetadataList.size} synced.")
        }

        val storedVideoIds =( if(videoStore.exists) videoStore.get() else emptyList()).map{it.id}.toSet()
        if(storedVideoIds.isNotEmpty()){
            val videoToDateMap = queryVideoIdDateMap(application)
            val videoMetadataList = videoToDateMap.mapNotNull{
                if (it.key !in storedVideoIds) return@mapNotNull null
                MediaMetadata(id=it.key, dateAdded = it.value, type = MediaType.VIDEO)
            }
            metadataRepo.insert(videoMetadataList)
            Log.d(TAG, "Video metadata sync complete. ${videoMetadataList.size} synced.")

        }


        sharedPrefs.edit {
            putBoolean(PrefsKeys.MEDIA_METADATA_SYNC_COMPLETE, true)
        }
    }

    private suspend fun transfer(application: Application, newDb: MediaDatabase) = withContext(Dispatchers.IO) {
        val newTagsRepository = TagRepository(newDb.tagDao())
        val newTagsCrossRefRepository = TagCrossRefRepository( newDb.tagCrossRefDao())
        val metadataRepo = MediaMetadataRepository(newDb.metadataDao())

        // Images
        val oldImageDb = ImageTagDatabase.getDatabase(application)
        val oldImageTagRepository = ImageTagRepository(oldImageDb.tagDao())
        val oldImageTagCrossRefRepository = ImageTagCrossRefRepository( oldImageDb.imageTagCrossRefDao())
        val imageTags = oldImageTagRepository.getAllTags()
        val imageCrossRefs = oldImageTagCrossRefRepository.getAllCrossRefs()

        val imageTagIds = newTagsRepository.insertTags(imageTags.map{ Tag(name=it.name, lastUsedAt = it.lastUsedAt)})
        val nameToImageTagId = imageTagIds
            .zip(imageTags.map { it.name })
            .filter { it.first != -1L }
            .associate { it.second to it.first }

        val validImageIds = metadataRepo.getByType(MediaType.IMAGE).map{it.id}.toSet()
        val updatedImageCrossRefs = imageCrossRefs.mapNotNull {
            val tagId = nameToImageTagId[it.tag] ?: return@mapNotNull null
            if (it.imageId !in validImageIds) return@mapNotNull null

            TagCrossRef(
                mediaId = it.imageId,
                tagId = tagId,
            )
        }

        newTagsCrossRefRepository.insertTagCrossRefs(updatedImageCrossRefs)
        oldImageDb.close()

        Log.d(TAG, "Image transfer complete. ${imageTagIds.size} tags transferred. ${updatedImageCrossRefs.size} cross refs transferred.")


        // Videos
        val oldVideoDb = VideoTagDatabase.getDatabase(application)
        val oldVideoTagRepository = VideoTagRepository(oldVideoDb.tagDao())
        val oldVideoTagCrossRefRepository = VideoTagCrossRefRepository( oldVideoDb.videoTagCrossRefDao())
        val videoTags = oldVideoTagRepository.getAllTags()
        val videoCrossRefs = oldVideoTagCrossRefRepository.getAllCrossRefs()

        val videoTagIds = newTagsRepository.insertTags(videoTags.map{ Tag(name=it.name, lastUsedAt = it.lastUsedAt)})
        val nameToVideoTagId = videoTagIds
            .zip(videoTags.map { it.name })
            .filter { it.first != -1L }
            .associate { it.second to it.first }

        val validVideoIds = metadataRepo.getByType(MediaType.VIDEO).map{it.id}.toSet()
        val updatedVideoCrossRefs = videoCrossRefs.mapNotNull {
            val tagId = nameToVideoTagId[it.tag] ?: return@mapNotNull null
            if (it.videoId !in validVideoIds) return@mapNotNull null

            TagCrossRef(
                mediaId = it.videoId,
                tagId = tagId,
            )
        }

        newTagsCrossRefRepository.insertTagCrossRefs(updatedVideoCrossRefs)
        oldVideoDb.close()

        Log.d(TAG, "Video transfer complete. ${videoTagIds.size} tags transferred. ${updatedVideoCrossRefs.size} cross refs transferred.")
    }
}