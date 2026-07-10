package com.fpf.smartscan.data.metadata

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.fpf.smartscan.data.MediaIdType
import com.fpf.smartscan.media.MediaType

@Dao
interface MediaMetadataDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(items: List<MediaMetadata>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(item: MediaMetadata)

    @Update
    suspend fun update(items: List<MediaMetadata>)

    @Update
    suspend fun update(item: MediaMetadata)


    @Query("SELECT * FROM media_metadata WHERE id IN (:mediaIds) AND type = :type")
    suspend fun getByIds(mediaIds: List<Long>, type: MediaType): List<MediaMetadata>


    @Query("SELECT * FROM media_metadata WHERE type = :type")
    suspend fun getByType(type: MediaType): List<MediaMetadata>

    @Query("SELECT id FROM media_metadata WHERE type = :type")
    suspend fun getIdsByType(type: MediaType): List<Long>


    @Query("""
        SELECT id, type
        FROM media_metadata
        WHERE NOT EXISTS (
            SELECT 1
            FROM media_cluster_crossref c
            WHERE c.mediaId = media_metadata.id
              AND c.mediaType = media_metadata.type
        )
    """)
    suspend fun getUnclusteredItemIds(): List<MediaIdType>


    // TAG QUERIES

    @Query("""
        SELECT m.*
        FROM media_metadata m
        INNER JOIN tag_crossref c 
            ON c.mediaId = m.id
            AND c.mediaType = m.type
        WHERE c.tagId = :tagId
        ORDER BY m.dateAdded DESC, m.id DESC
        LIMIT :limit OFFSET :offset
    """)
    suspend fun getByTag(tagId: Long, limit: Int, offset: Int): List<MediaMetadata>


    @Query("""
        SELECT m.*
        FROM media_metadata m
        INNER JOIN tag_crossref c 
            ON c.mediaId = m.id
            AND c.mediaType = m.type
        WHERE c.tagId = :tagId
        ORDER BY m.dateAdded DESC, m.id DESC
    """)
    suspend fun getByTag(tagId: Long): List<MediaMetadata>


    @Query("""
        SELECT m.*
        FROM media_metadata m
        INNER JOIN tag_crossref c 
            ON c.mediaId = m.id
            AND c.mediaType = m.type
        WHERE c.tagId = :tagId
          AND m.type = :type
        ORDER BY m.dateAdded DESC, m.id DESC
        LIMIT :limit OFFSET :offset
    """)
    suspend fun getByTag(tagId: Long, type: MediaType, limit: Int, offset: Int): List<MediaMetadata>


    @Query("""
        SELECT m.*
        FROM media_metadata m
        INNER JOIN tag_crossref c 
            ON c.mediaId = m.id
            AND c.mediaType = m.type
        WHERE c.tagId = :tagId
          AND m.type = :type
        ORDER BY m.dateAdded DESC, m.id DESC
    """)
    suspend fun getByTag(tagId: Long, type: MediaType): List<MediaMetadata>


    @Query("""
        SELECT m.*
        FROM media_metadata m
        INNER JOIN tag_crossref c 
            ON c.mediaId = m.id
            AND c.mediaType = m.type
        WHERE c.tagId = :tagId
          AND m.type = :type
          AND (:startDate IS NULL OR m.dateAdded >= :startDate)
          AND (:endDate IS NULL OR m.dateAdded <= :endDate)
        ORDER BY m.dateAdded DESC, m.id DESC
    """)
    suspend fun getByTag(tagId: Long, type: MediaType, startDate: Long?, endDate: Long?): List<MediaMetadata>

    // CLUSTER QUERIES

    @Query("""
        SELECT m.*
        FROM media_metadata m
        INNER JOIN media_cluster_crossref c
            ON c.mediaId = m.id
            AND c.mediaType = m.type
        WHERE c.clusterId = :clusterId
        ORDER BY m.dateAdded DESC, m.id DESC
        LIMIT :limit OFFSET :offset
    """)
    suspend fun getByCluster(clusterId: Long, limit: Int, offset: Int): List<MediaMetadata>


    @Query("""
        SELECT m.*
        FROM media_metadata m
        INNER JOIN media_cluster_crossref c
            ON c.mediaId = m.id
            AND c.mediaType = m.type
        WHERE c.clusterId = :clusterId
        ORDER BY m.dateAdded DESC, m.id DESC
    """)
    suspend fun getByCluster(clusterId: Long): List<MediaMetadata>



    @Query("""
        SELECT m.*
        FROM media_metadata m
        INNER JOIN media_cluster_crossref c
            ON c.mediaId = m.id
            AND c.mediaType = m.type
        WHERE c.clusterId = :clusterId
          AND m.type = :type
        ORDER BY m.dateAdded DESC, m.id DESC
        LIMIT :limit OFFSET :offset
    """)
    suspend fun getByCluster(clusterId: Long, type: MediaType, limit: Int, offset: Int): List<MediaMetadata>
    @Transaction
    @Query("""
        DELETE FROM media_metadata
        WHERE id IN (:mediaIds)
          AND type = :type
    """)
    suspend fun deleteByIds(mediaIds: List<Long>, type: MediaType)

    @Query("DELETE FROM media_metadata")
    suspend fun clear()
}