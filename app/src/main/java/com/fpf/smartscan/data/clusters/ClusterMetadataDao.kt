package com.fpf.smartscan.data.clusters

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ClusterMetadataDao {
    @Query("""
    SELECT 
        c.clusterId,
        c.label,
        COUNT(ref.mediaId) AS prototypeSize,
        m.id AS thumbNailId,
        m.type AS thumbNailType
    FROM cluster_metadata c
    JOIN media_cluster_crossref ref
        ON ref.clusterId = c.clusterId
    JOIN media_metadata m
        ON m.id = ref.mediaId
        AND m.type = ref.mediaType
    WHERE m.id = (
        SELECT m2.id
        FROM media_metadata m2
        INNER JOIN media_cluster_crossref ref2
            ON ref2.mediaId = m2.id
            AND ref2.mediaType = m2.type
        WHERE ref2.clusterId = c.clusterId
        ORDER BY m2.dateAdded DESC, m2.id DESC
        LIMIT 1
    )
    GROUP BY c.clusterId
    ORDER BY prototypeSize DESC
""")
    fun getCollections(): Flow<List<AutoCollectionData>>

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

