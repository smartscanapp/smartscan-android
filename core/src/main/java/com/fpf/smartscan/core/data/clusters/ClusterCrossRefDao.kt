package com.fpf.smartscan.core.data.clusters

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.fpf.smartscan.core.media.MediaType

@Dao
interface ClusterCrossRefDao {

    @Query("SELECT * FROM media_cluster_crossref")
    suspend fun getAll(): List<ClusterCrossRefEntity>

    @Query("""
    SELECT crossRef.*
    FROM media_cluster_crossref crossRef
    JOIN media_metadata metadata ON metadata.id = crossRef.mediaId
    WHERE metadata.type = :type
    """)
    suspend fun getByType(type: MediaType): List<ClusterCrossRefEntity>

    @Query("SELECT * FROM media_cluster_crossref WHERE clusterId in (:ids)")
    suspend fun getByClusterIds(ids: List<Long>): List<ClusterCrossRefEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(crossRefs: List<ClusterCrossRefEntity>)

    @Query("DELETE FROM media_cluster_crossref")
    suspend fun clear()

    @Query("SELECT COUNT(*) FROM media_cluster_crossref")
    suspend fun count(): Int

    @Query("SELECT COUNT(*) FROM media_cluster_crossref WHERE clusterId = :clusterId")
    suspend fun countByClusterId(clusterId: Long): Int
}