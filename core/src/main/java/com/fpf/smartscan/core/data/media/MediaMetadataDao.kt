package com.fpf.smartscan.core.data.media

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.fpf.smartscan.core.data.MediaIdType
import com.fpf.smartscan.core.media.MediaType

@Dao
interface MediaMetadataDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(items: List<MediaMetadataEntity>)

    @Update
    suspend fun update(items: List<MediaMetadataEntity>)

    @Query("""
        SELECT * 
        FROM media_metadata 
        WHERE id IN (:mediaIds) 
            AND type = :type 
            AND isTrashed = :isTrashed
            AND (:isDuplicate IS NULL OR isDuplicate = :isDuplicate)
            """
    )
    suspend fun getByIds(mediaIds: List<Long>, type: MediaType, isTrashed: Boolean, isDuplicate: Boolean?): List<MediaMetadataEntity>

    @Query("""
        SELECT * 
        FROM media_metadata 
        WHERE type = :type 
            AND isTrashed = :isTrashed
            AND (:isDuplicate IS NULL OR isDuplicate = :isDuplicate)
            """
    )
    suspend fun getByType(type: MediaType, isTrashed: Boolean, isDuplicate: Boolean?): List<MediaMetadataEntity>

    @Query("""
        SELECT id 
        FROM media_metadata 
        WHERE type = :type 
            AND isTrashed = :isTrashed
            AND (:isDuplicate IS NULL OR isDuplicate = :isDuplicate)
            """
    )
    suspend fun getIdsByType(type: MediaType, isTrashed: Boolean, isDuplicate: Boolean?): List<Long>

    @Query("""
        SELECT id, type
        FROM media_metadata
        WHERE isTrashed = 0
            AND NOT EXISTS (
                SELECT 1
                FROM media_cluster_crossref c
                WHERE c.mediaId = media_metadata.id
                AND c.mediaType = media_metadata.type
        )
        """
    )
    suspend fun getUnclusteredItemIds(): List<MediaIdType>

    // TAG QUERIES

    @Query("""
    SELECT m.*
    FROM media_metadata m
    INNER JOIN tag_crossref c
        ON c.mediaId = m.id
        AND c.mediaType = m.type
    WHERE c.tagId = :tagId
        AND m.isTrashed = 0
        AND (:mediaType IS NULL OR m.type = :mediaType)
        AND (:startDate IS NULL OR m.dateAdded >= :startDate)
        AND (:endDate IS NULL OR m.dateAdded <= :endDate)
        AND (:isDuplicate IS NULL OR m.isDuplicate = :isDuplicate)
    ORDER BY m.dateAdded DESC, m.id DESC
    LIMIT :limit OFFSET :offset
""")
    suspend fun getByTagDesc(
        tagId: Long,
        limit: Int,
        offset: Int,
        mediaType: MediaType?,
        startDate: Long?,
        endDate: Long?,
        isDuplicate: Boolean?
    ): List<MediaMetadataEntity>

    @Query("""
    SELECT m.*
    FROM media_metadata m
    INNER JOIN tag_crossref c
        ON c.mediaId = m.id
        AND c.mediaType = m.type
    WHERE c.tagId = :tagId
        AND m.isTrashed = 0
        AND (:mediaType IS NULL OR m.type = :mediaType)
        AND (:startDate IS NULL OR m.dateAdded >= :startDate)
        AND (:endDate IS NULL OR m.dateAdded <= :endDate)
        AND (:isDuplicate IS NULL OR m.isDuplicate = :isDuplicate)
    ORDER BY m.dateAdded ASC, m.id ASC
    LIMIT :limit OFFSET :offset
""")
    suspend fun getByTagAsc(
        tagId: Long,
        limit: Int,
        offset: Int,
        mediaType: MediaType?,
        startDate: Long?,
        endDate: Long?,
        isDuplicate: Boolean?,
    ): List<MediaMetadataEntity>


    @Query("""
    SELECT m.*
    FROM media_metadata m
    INNER JOIN tag_crossref c
        ON c.mediaId = m.id
        AND c.mediaType = m.type
    WHERE c.tagId = :tagId
        AND m.isTrashed = 0
        AND (:mediaType IS NULL OR m.type = :mediaType)
        AND (:startDate IS NULL OR m.dateAdded >= :startDate)
        AND (:endDate IS NULL OR m.dateAdded <= :endDate)
        AND (:isDuplicate IS NULL OR m.isDuplicate = :isDuplicate)
    ORDER BY m.dateAdded DESC, m.id DESC
""")
    suspend fun getByTag(
        tagId: Long,
        mediaType: MediaType?,
        startDate: Long?,
        endDate: Long?,
        isDuplicate: Boolean?
    ): List<MediaMetadataEntity>


    @Query("""
    SELECT DISTINCT m.*
    FROM media_metadata m
    INNER JOIN tag_crossref c
        ON c.mediaId = m.id
        AND c.mediaType = m.type
    WHERE c.tagId IN (:tagIds)
        AND m.description IS NULL
        AND m.isTrashed = 0
        AND (:mediaType IS NULL OR m.type = :mediaType)
""")
    suspend fun getByTagsWithoutDescription(
        tagIds: List<Long>,
        mediaType: MediaType?
    ): List<MediaMetadataEntity>

    // CLUSTER QUERIES

    @Query("""
    SELECT m.*
    FROM media_metadata m
    INNER JOIN media_cluster_crossref c
        ON c.mediaId = m.id
        AND c.mediaType = m.type
    WHERE c.clusterId = :clusterId
        AND m.isTrashed = 0
        AND (:mediaType IS NULL OR m.type = :mediaType)
        AND (:isDuplicate IS NULL OR m.isDuplicate = :isDuplicate)
    ORDER BY m.dateAdded DESC, m.id DESC
    LIMIT :limit OFFSET :offset
""")
    suspend fun getByClusterDesc(
        clusterId: Long,
        limit: Int,
        offset: Int,
        mediaType: MediaType?,
        isDuplicate: Boolean?
    ): List<MediaMetadataEntity>

    @Query("""
    SELECT m.*
    FROM media_metadata m
    INNER JOIN media_cluster_crossref c
        ON c.mediaId = m.id
        AND c.mediaType = m.type
    WHERE c.clusterId = :clusterId
        AND m.isTrashed = 0
        AND (:mediaType IS NULL OR m.type = :mediaType)
        AND (:isDuplicate IS NULL OR m.isDuplicate = :isDuplicate)
    ORDER BY m.dateAdded ASC, m.id ASC
    LIMIT :limit OFFSET :offset
""")
    suspend fun getByClusterAsc(
        clusterId: Long,
        limit: Int,
        offset: Int,
        mediaType: MediaType?,
        isDuplicate: Boolean?
    ): List<MediaMetadataEntity>

    @Query("""
    SELECT m.*
    FROM media_metadata m
    INNER JOIN media_cluster_crossref c
        ON c.mediaId = m.id
        AND c.mediaType = m.type
    WHERE c.clusterId = :clusterId
        AND m.isTrashed = 0
        AND (:mediaType IS NULL OR m.type = :mediaType)
        AND (:isDuplicate IS NULL OR m.isDuplicate = :isDuplicate)
    ORDER BY m.dateAdded DESC, m.id DESC
""")
    suspend fun getByCluster(
        clusterId: Long,
        mediaType: MediaType?,
        isDuplicate: Boolean?
    ): List<MediaMetadataEntity>


    @Query("""
    SELECT m.*
    FROM media_metadata m
    INNER JOIN media_cluster_crossref c
        ON c.mediaId = m.id
        AND c.mediaType = m.type
    WHERE c.clusterId IN (:clusterIds)
        AND m.isTrashed = 0
        AND m.description IS NULL
        AND (:mediaType IS NULL OR m.type = :mediaType)
""")
    suspend fun getByClustersWithoutDescription(
        clusterIds: List<Long>,
        mediaType: MediaType?
    ): List<MediaMetadataEntity>

    // Concept queries
    @Query("""
    SELECT m.*
    FROM media_metadata m
    INNER JOIN concept_crossref c
        ON c.mediaId = m.id
        AND c.mediaType = m.type
    WHERE c.conceptId = :conceptId
        AND m.isTrashed = 0
        AND (:mediaType IS NULL OR m.type = :mediaType)
        AND (:minSimilarity IS NULL OR c.similarity >= :minSimilarity)
    ORDER BY m.dateAdded DESC, m.id DESC
    LIMIT :limit OFFSET :offset
""")
    suspend fun getByConceptSortedByDateDesc(
        conceptId: Long,
        mediaType: MediaType?,
        minSimilarity: Float?,
        limit: Int,
        offset: Int
    ): List<MediaMetadataEntity>

    @Query("""
    SELECT m.*
    FROM media_metadata m
    INNER JOIN concept_crossref c
        ON c.mediaId = m.id
        AND c.mediaType = m.type
    WHERE c.conceptId = :conceptId
        AND m.isTrashed = 0
        AND (:mediaType IS NULL OR m.type = :mediaType)
        AND (:minSimilarity IS NULL OR c.similarity >= :minSimilarity)
    ORDER BY m.dateAdded ASC, m.id ASC
    LIMIT :limit OFFSET :offset
""")
    suspend fun getByConceptSortedByDateAsc(
        conceptId: Long,
        limit: Int,
        offset: Int,
        mediaType: MediaType?,
        minSimilarity: Float?,
    ): List<MediaMetadataEntity>


    @Query("""
    SELECT m.*
    FROM media_metadata m
    INNER JOIN concept_crossref c
        ON c.mediaId = m.id
        AND c.mediaType = m.type
    WHERE c.conceptId = :conceptId
      AND (:mediaType IS NULL OR m.type = :mediaType)
      AND (:minSimilarity IS NULL OR c.similarity >= :minSimilarity)
    ORDER BY m.dateAdded DESC, m.id DESC
""")
    suspend fun getByConceptSortedByDate(
        conceptId: Long,
        mediaType: MediaType?,
        minSimilarity: Float?
    ): List<MediaMetadataEntity>


    @Query("""
    SELECT m.*
    FROM media_metadata m
    INNER JOIN concept_crossref c
        ON c.mediaId = m.id
        AND c.mediaType = m.type
    WHERE c.conceptId = :conceptId
        AND m.isTrashed = 0
        AND (:mediaType IS NULL OR m.type = :mediaType)
        AND (:minSimilarity IS NULL OR c.similarity >= :minSimilarity)
    ORDER BY c.similarity DESC, m.id DESC
    LIMIT :limit OFFSET :offset
""")
    suspend fun getByConceptSortedBySimilarityDesc(
        conceptId: Long,
        limit: Int,
        offset: Int,
        mediaType: MediaType?,
        minSimilarity: Float?,
    ): List<MediaMetadataEntity>

    @Query("""
    SELECT m.*
    FROM media_metadata m
    INNER JOIN concept_crossref c
        ON c.mediaId = m.id
        AND c.mediaType = m.type
    WHERE c.conceptId = :conceptId
        AND m.isTrashed = 0
        AND (:mediaType IS NULL OR m.type = :mediaType)
        AND (:minSimilarity IS NULL OR c.similarity >= :minSimilarity)
    ORDER BY c.similarity ASC, m.id ASC
    LIMIT :limit OFFSET :offset
""")
    suspend fun getByConceptSortedBySimilarityAsc(
        conceptId: Long,
        limit: Int,
        offset: Int,
        mediaType: MediaType?,
        minSimilarity: Float?,
    ): List<MediaMetadataEntity>


    @Query("""
    SELECT m.*
    FROM media_metadata m
    INNER JOIN concept_crossref c
        ON c.mediaId = m.id
        AND c.mediaType = m.type
    WHERE c.conceptId = :conceptId
        AND m.isTrashed = 0
        AND (:mediaType IS NULL OR m.type = :mediaType)
        AND (:minSimilarity IS NULL OR c.similarity >= :minSimilarity)
    ORDER BY c.similarity DESC, m.id DESC
""")
    suspend fun getByConceptSortedBySimilarity(
        conceptId: Long,
        mediaType: MediaType?,
        minSimilarity: Float?
    ): List<MediaMetadataEntity>

    @Query("""
        DELETE FROM media_metadata
        WHERE id IN (:mediaIds)
          AND type = :type
    """)
    suspend fun deleteByIds(mediaIds: List<Long>, type: MediaType)

    @Query("DELETE FROM media_metadata")
    suspend fun clear()

    @Query("""
    UPDATE media_metadata
    SET isDuplicate = 1
    WHERE id IN (:mediaIds)
      AND type = :type
""")
    suspend fun markDuplicates(mediaIds: List<Long>, type: MediaType)
}