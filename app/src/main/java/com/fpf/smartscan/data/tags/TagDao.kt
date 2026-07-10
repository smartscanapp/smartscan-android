package com.fpf.smartscan.data.tags

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
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

    @Query("""
    SELECT
        t.id AS tagId,
        t.name,
        counts.size,
        latest.thumbNailId,
        latest.thumbNailType
    FROM media_tag t
    JOIN (
        SELECT
            tagId,
            COUNT(*) AS size
        FROM tag_crossref
        GROUP BY tagId
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
        JOIN media_metadata m
            ON m.id = c.mediaId
            AND m.type = c.mediaType
    ) latest
        ON latest.tagId = t.id
        AND latest.rn = 1
    ORDER BY counts.size DESC
""")
    fun getCollections(): Flow<List<TagCollectionData>>

    @Query("""
    SELECT
        t.id AS tagId,
        t.name,
        counts.size,
        latest.thumbNailId,
        latest.thumbNailType
    FROM media_tag t
    JOIN (
        SELECT
            tagId,
            COUNT(*) AS size
        FROM tag_crossref
        GROUP BY tagId
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
        JOIN media_metadata m
            ON m.id = c.mediaId
            AND m.type = c.mediaType
    ) latest
        ON latest.tagId = t.id
        AND latest.rn = 1
    WHERE t.id IN (:tagIds)
    ORDER BY counts.size DESC
""")
    fun getCollections(tagIds: List<Long>): List<TagCollectionData>


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