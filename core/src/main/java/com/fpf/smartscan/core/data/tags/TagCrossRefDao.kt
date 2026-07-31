package com.fpf.smartscan.core.data.tags

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.fpf.smartscan.core.media.MediaType

@Dao
interface TagCrossRefDao {
    @Query("SELECT * FROM tag_crossref")
    suspend fun getAllCrossRefs(): List<TagCrossRefEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(tags: List<TagCrossRefEntity>)

    @Transaction
    @Query("""
    INSERT OR IGNORE INTO tag_crossref(mediaId, mediaType, tagId)
    SELECT mediaId, mediaType, :primaryTagId
    FROM tag_crossref
    WHERE tagId IN (:tagIds)
""")
    suspend fun moveCrossRefs(primaryTagId: Long, tagIds: List<Long>)

    @Query(""" DELETE FROM tag_crossref WHERE mediaId IN (:mediaIds) AND mediaType = :mediaType AND tagId = :tagId """)
    suspend fun deleteMediaMatchingTag(mediaIds: List<Long>, mediaType: MediaType, tagId: Long)
    @Query("DELETE FROM tag_crossref WHERE tagId IN (:tagIds)")
    suspend fun deleteByTags(tagIds: List<Long>)

    @Query("DELETE FROM tag_crossref")
    suspend fun clear()
}