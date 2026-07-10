package com.fpf.smartscan.data.clusters

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.fpf.smartscan.media.MediaType
import kotlinx.coroutines.flow.Flow

@Dao
interface ClusterCrossRefDao {

    @Query("SELECT * FROM media_cluster_crossref")
    suspend fun getAll(): List<ClusterCrossRef>

    @Query("""
    SELECT crossRef.*
    FROM media_cluster_crossref crossRef
    JOIN media_metadata metadata ON metadata.id = crossRef.mediaId
    WHERE metadata.type = :type
    """)
    suspend fun getByType(type: MediaType): List<ClusterCrossRef>

    @Query("SELECT * FROM media_cluster_crossref WHERE clusterId in (:ids)")
    suspend fun getByClusterIds(ids: List<Long>): List<ClusterCrossRef>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(crossRefs: List<ClusterCrossRef>)

    @Query("DELETE FROM media_cluster_crossref")
    suspend fun clear()

    @Query("SELECT COUNT(*) FROM media_cluster_crossref")
    suspend fun count(): Int

    @Query("SELECT COUNT(*) FROM media_cluster_crossref WHERE clusterId = :clusterId")
    suspend fun countByClusterId(clusterId: Long): Int
}