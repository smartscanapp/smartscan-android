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
    fun getAllFlow(): Flow<List<Tag>>

    @Query("SELECT * FROM media_tag")
    suspend fun getAll(): List<Tag>

    @Query("SELECT * FROM media_tag WHERE name in (:names)")
    suspend fun getByNames(names: List<String>): List<Tag>

    @Query("SELECT * FROM media_tag WHERE id in (:ids)")
    suspend fun getByIds(ids: List<Long>): List<Tag>

    @Query("""
    SELECT 
        t.id AS tagId,
        t.name,
        COUNT(c.mediaId) AS size,
        m.id AS thumbNailId,
        m.type AS thumbNailType
    FROM media_tag t
    JOIN tag_crossref c
        ON c.tagId = t.id
    JOIN media_metadata m
        ON m.id = c.mediaId
        AND m.type = c.mediaType
    WHERE m.id = (
        SELECT m2.id
        FROM media_metadata m2
        INNER JOIN tag_crossref c2
            ON c2.mediaId = m2.id
            AND c2.mediaType = m2.type
        WHERE c2.tagId = t.id
        ORDER BY m2.dateAdded DESC, m2.id DESC
        LIMIT 1
    )
    GROUP BY t.id
    ORDER BY size DESC
""")
    fun getCollections(): Flow<List<TagCollectionData>>

    // MUST use ignore. Using replace will cause cascading deletes of cross refs
    @Transaction
    @Insert(onConflict = OnConflictStrategy.Companion.IGNORE)
    suspend fun insert(imageTags: List<Tag>): List<Long>

    @Update
    suspend fun update(imageTags: List<Tag>)

    @Delete
    suspend fun delete(imageTags: List<Tag>)

    @Query("DELETE FROM media_tag WHERE name in (:names)")
    suspend fun deleteByNames(names: List<String>)

    @Query("DELETE FROM media_tag WHERE id in (:ids)")
    suspend fun deleteByIds(ids: List<Long>)

    @Query("DELETE FROM media_tag")
    suspend fun clear()
}