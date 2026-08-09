package com.fpf.smartscan.core.data.clusters

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.fpf.smartscan.core.media.MediaType
import kotlinx.coroutines.flow.Flow


// Crossref count used as prototypeSize to always use crossrefs as source of truth
// and importantly so Flow automatically retriggers when crossrefs change
@Dao
interface ClusterMetadataDao {

    @Query(
        """
        SELECT meta.*
        FROM cluster_metadata AS meta
        INNER JOIN media_cluster_crossref AS crossref
            ON meta.clusterId = crossref.clusterId
        WHERE crossref.mediaId = :mediaId
          AND crossref.mediaType = :mediaType
        """
    )
    suspend fun getClustersForMedia(mediaId: Long, mediaType: MediaType): List<ClusterMetadataEntity>

    @Query("""
WITH active_media AS (
    SELECT id, type, dateAdded
    FROM media_metadata
    WHERE isTrashed = 0
)
SELECT 
    c.clusterId,
    c.label,
    COUNT(ref.mediaId) AS prototypeSize,
    latest.mediaId AS thumbNailId,
    latest.mediaType AS thumbNailType
FROM cluster_metadata c
JOIN media_cluster_crossref ref
    ON ref.clusterId = c.clusterId
JOIN active_media m
    ON m.id = ref.mediaId
    AND m.type = ref.mediaType
JOIN (
    SELECT
        ref.clusterId,
        m.id AS mediaId,
        m.type AS mediaType,
        ROW_NUMBER() OVER (
            PARTITION BY ref.clusterId
            ORDER BY m.dateAdded DESC, m.id DESC
        ) AS rowNum
    FROM media_cluster_crossref ref
    JOIN active_media m
        ON m.id = ref.mediaId
        AND m.type = ref.mediaType
) latest
    ON latest.clusterId = c.clusterId
    AND latest.rowNum = 1
GROUP BY c.clusterId
ORDER BY prototypeSize DESC
""")
    fun getCollections(): Flow<List<AutoCollectionData>>

    @Query("""
WITH active_media AS (
    SELECT id, type, dateAdded
    FROM media_metadata
    WHERE isTrashed = 0
)
SELECT 
    c.clusterId,
    c.label,
    COUNT(ref.mediaId) AS prototypeSize,
    latest.mediaId AS thumbNailId,
    latest.mediaType AS thumbNailType
FROM cluster_metadata c
JOIN media_cluster_crossref ref
    ON ref.clusterId = c.clusterId
JOIN active_media m
    ON m.id = ref.mediaId
    AND m.type = ref.mediaType
JOIN (
    SELECT
        ref.clusterId,
        m.id AS mediaId,
        m.type AS mediaType,
        ROW_NUMBER() OVER (
            PARTITION BY ref.clusterId
            ORDER BY m.dateAdded DESC, m.id DESC
        ) AS rowNum
    FROM media_cluster_crossref ref
    JOIN active_media m
        ON m.id = ref.mediaId
        AND m.type = ref.mediaType
) latest
    ON latest.clusterId = c.clusterId
    AND latest.rowNum = 1
WHERE c.clusterId IN (:clusterIds)
GROUP BY c.clusterId
ORDER BY prototypeSize DESC
""")
    fun getCollections(clusterIds: List<Long>): List<AutoCollectionData>



    @Query("""
    SELECT metadata.*, COUNT(crossRef.mediaId) AS prototypeSize
    FROM cluster_metadata metadata
    JOIN media_cluster_crossref crossRef ON metadata.clusterId = crossRef.clusterId
    GROUP BY metadata.clusterId
""")
    suspend fun get(): List<ClusterMetadataEntity>

    @Query("""
    SELECT metadata.*, COUNT(crossRef.mediaId) AS prototypeSize
    FROM cluster_metadata metadata
    JOIN media_cluster_crossref crossRef ON metadata.clusterId = crossRef.clusterId
    WHERE metadata.clusterId IN (:ids)
    GROUP BY metadata.clusterId
""")
    suspend fun get(ids: List<Long>): List<ClusterMetadataEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(clusters: List<ClusterMetadataEntity>): List<Long>

    @Update
    suspend fun update(clusters: List<ClusterMetadataEntity>)

    @Transaction
    @Query("DELETE FROM cluster_metadata WHERE clusterId IN (:ids)")
    suspend fun delete(ids: List<Long>)

    @Query("SELECT COUNT(*) FROM cluster_metadata WHERE prototypeSize >= :minSize")
    suspend fun count(minSize: Int = 1): Int

    @Query("DELETE FROM cluster_metadata")
    suspend fun clear()

}

