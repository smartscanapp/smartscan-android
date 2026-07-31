package com.fpf.smartscan.core.data.clusters

import com.fpf.smartscan.core.cluster.ClusterCrossRef
import com.fpf.smartscan.core.data.mappers.toDomain
import com.fpf.smartscan.core.data.mappers.toEntity
import com.fpf.smartscan.core.media.MediaType

class ClusterCrossRefRepository(private val dao: ClusterCrossRefDao) {
    private var clusterToMediaIdsMap: MutableMap<Pair<Long, MediaType>, MutableSet<Long>> = mutableMapOf()
    private var refreshCache: Boolean = false

    suspend fun getAllCrossRefs(): List<ClusterCrossRef> = dao.getAll().map{it.toDomain()}
    suspend fun getByType(mediaType: MediaType): List<ClusterCrossRef> = dao.getByType(mediaType).map{it.toDomain()}
    suspend fun getByClusterIds(ids: List<Long>):  List<ClusterCrossRef> = dao.getByClusterIds(ids).map { it.toDomain() }
    suspend fun upsertClusterCrossRefs(crossRefs: List<ClusterCrossRef>) {
        dao.upsert(crossRefs.map { it.toEntity() })
        refreshCache = true
    }

    suspend fun clear() {
        refreshCache = false
        clusterToMediaIdsMap.clear()
        dao.clear()
    }
    suspend fun count() = dao.count()
    suspend fun count(clusterId: Long) = dao.countByClusterId(clusterId)

    suspend fun getClusterToMediaIdsMap(): Map<Pair<Long, MediaType>, MutableSet<Long>> {
        if(refreshCache){
            clusterToMediaIdsMap.clear()
            refreshCache = false
        }
        if (clusterToMediaIdsMap.isNotEmpty()) return clusterToMediaIdsMap

        for(ref in getAllCrossRefs()){
            clusterToMediaIdsMap.computeIfAbsent(Pair(ref.clusterId, ref.mediaType)) { HashSet() }.add(ref.mediaId)
        }
        return clusterToMediaIdsMap
    }

}