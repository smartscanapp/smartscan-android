package com.fpf.smartscan.data.tags

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.fpf.smartscan.media.MediaType

@Dao
interface TagCrossRefDao {

    @Query("SELECT DISTINCT tagId FROM tag_crossref WHERE mediaId = :mediaId")
    suspend fun getTagsForMedia(mediaId: Long): List<Long>

    @Query("SELECT * FROM tag_crossref")
    suspend fun getAllCrossRefs(): List<TagCrossRefEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(tags: List<TagCrossRefEntity>)

    @Query(""" DELETE FROM tag_crossref WHERE mediaId IN (:mediaIds) AND mediaType = :mediaType AND tagId = :tagId """)
    suspend fun deleteMediaMatchingTag(mediaIds: List<Long>, mediaType: MediaType, tagId: Long)
    @Query("DELETE FROM tag_crossref WHERE tagId IN (:tagIds)")
    suspend fun deleteByTags(tagIds: List<Long>)

    @Query("DELETE FROM tag_crossref")
    suspend fun clear()
}