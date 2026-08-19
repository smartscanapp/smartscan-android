package com.fpf.smartscan.core.data.tags

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.fpf.smartscan.core.media.MediaType
import kotlinx.coroutines.flow.Flow


@Dao
interface TagDao {
    @Query("SELECT * FROM media_tag")
    fun getAllFlow(): Flow<List<TagEntity>>

    @Query("SELECT * FROM media_tag")
    suspend fun getAll(): List<TagEntity>

    @Query("SELECT * FROM media_tag WHERE name in (:names)")
    suspend fun getByNames(names: List<String>): List<TagEntity>

    @Query("SELECT * FROM media_tag WHERE id in (:ids)")
    suspend fun getByIds(ids: List<Long>): List<TagEntity>


    @Query(
        """
        SELECT tag.*
        FROM media_tag AS tag
        INNER JOIN tag_crossref AS crossref
            ON tag.id = crossref.tagId
        WHERE crossref.mediaId = :mediaId
          AND crossref.mediaType = :mediaType
        """
    )
    suspend fun getTagsForMedia(mediaId: Long, mediaType: MediaType): List<TagEntity>

    // Crossref count used as size to always use crossrefs as source of truth
    // and importantly so Flow automatically retriggers when crossrefs change
    @Query("""
WITH active_media AS (
    SELECT id, type, dateAdded, isDuplicate
    FROM media_metadata
    WHERE isTrashed = 0
)
SELECT
    t.id AS tagId,
    t.name,
    counts.size,
    counts.imageCount,
    counts.duplicateImageCount,
    latest.thumbNailId,
    latest.thumbNailType
FROM media_tag t
JOIN (
    SELECT
        c.tagId,
        COUNT(*) AS size,
        COUNT(CASE WHEN m.type = 0 THEN 1 END) AS imageCount,
        COUNT(CASE WHEN m.type = 0 AND m.isDuplicate = 1 THEN 1 END) AS duplicateImageCount
    FROM tag_crossref c
    JOIN active_media m
        ON m.id = c.mediaId
        AND m.type = c.mediaType
    GROUP BY c.tagId
) counts
    ON counts.tagId = t.id
JOIN (
    SELECT
        c.tagId,
        m.id AS thumbNailId,
        m.type AS thumbNailType,
        ROW_NUMBER() OVER (
            PARTITION BY c.tagId
            ORDER BY m.dateAdded DESC, m.id DESC
        ) AS rn
    FROM tag_crossref c
    JOIN active_media m
        ON m.id = c.mediaId
        AND m.type = c.mediaType
) latest
    ON latest.tagId = t.id
    AND latest.rn = 1
ORDER BY counts.size DESC
""")
    fun getCollections(): Flow<List<TagCollectionData>>

    @Query("""
WITH active_media AS (
    SELECT id, type, dateAdded, isDuplicate
    FROM media_metadata
    WHERE isTrashed = 0
)
SELECT
    t.id AS tagId,
    t.name,
    counts.size,
    counts.imageCount,
    counts.duplicateImageCount,
    latest.thumbNailId,
    latest.thumbNailType
FROM media_tag t
JOIN (
    SELECT
        c.tagId,
        COUNT(*) AS size,
        COUNT(CASE WHEN m.type = 0 THEN 1 END) AS imageCount,
        COUNT(CASE WHEN m.type = 0 AND m.isDuplicate = 1 THEN 1 END) AS duplicateImageCount
    FROM tag_crossref c
    JOIN active_media m
        ON m.id = c.mediaId
        AND m.type = c.mediaType
    GROUP BY c.tagId
) counts
    ON counts.tagId = t.id
JOIN (
    SELECT
        c.tagId,
        m.id AS thumbNailId,
        m.type AS thumbNailType,
        ROW_NUMBER() OVER (
            PARTITION BY c.tagId
            ORDER BY m.dateAdded DESC, m.id DESC
        ) AS rn
    FROM tag_crossref c
    JOIN active_media m
        ON m.id = c.mediaId
        AND m.type = c.mediaType
) latest
    ON latest.tagId = t.id
    AND latest.rn = 1
WHERE t.id IN (:tagIds)
ORDER BY counts.size DESC
""")
    suspend fun getCollections(tagIds: List<Long>): List<TagCollectionData>


    // MUST use ignore. Using replace will cause cascading deletes of cross refs
    @Transaction
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(imageTags: List<TagEntity>): List<Long>

    @Update
    suspend fun update(imageTags: List<TagEntity>)

    @Delete
    suspend fun delete(imageTags: List<TagEntity>)

    @Query("DELETE FROM media_tag WHERE name in (:names)")
    suspend fun deleteByNames(names: List<String>)

    @Query("DELETE FROM media_tag WHERE id in (:ids)")
    suspend fun deleteByIds(ids: List<Long>)

    @Query("DELETE FROM media_tag")
    suspend fun clear()
}