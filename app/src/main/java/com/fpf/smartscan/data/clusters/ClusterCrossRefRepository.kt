package com.fpf.smartscan.data.clusters

import com.fpf.smartscan.cluster.ClusterCrossRef
import com.fpf.smartscan.data.mappers.toDomain
import com.fpf.smartscan.data.mappers.toEntity
import com.fpf.smartscan.media.MediaType

class ClusterCrossRefRepository(private val dao: ClusterCrossRefDao) {
    private var clusterToMediaIdsMap: MutableMap<Long, MutableSet<Long>> = mutableMapOf()
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

    //NOTE: may require changing foreign key is now (id, type) though this may not be necessary
    // because its only used in search and search around handles media types separate meaninng any collision between
    // MediaStre video ids and image ids arent possible.
    suspend fun getClusterToMediaIdsMap(): Map<Long, MutableSet<Long>> {
        if(refreshCache){
            clusterToMediaIdsMap.clear()
            refreshCache = false
        }
        if (clusterToMediaIdsMap.isNotEmpty()) return clusterToMediaIdsMap

        for(ref in getAllCrossRefs()){
            clusterToMediaIdsMap.computeIfAbsent(ref.clusterId) { HashSet() }.add(ref.mediaId)
        }
        return clusterToMediaIdsMap
    }

}