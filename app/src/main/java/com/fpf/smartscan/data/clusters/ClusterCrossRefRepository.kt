package com.fpf.smartscan.data.clusters

import com.fpf.smartscan.media.MediaType
import kotlinx.coroutines.flow.Flow

class ClusterCrossRefRepository(private val dao: ClusterCrossRefDao) {
    private var clusterToMediaIdsMap: MutableMap<Long, MutableSet<Long>> = mutableMapOf()
    private var refreshCache: Boolean = false

    suspend fun getAllCrossRefs(): List<ClusterCrossRef> = dao.getAll()
    suspend fun getByType(mediaType: MediaType): List<ClusterCrossRef> = dao.getByType(mediaType)
    suspend fun getByClusterIds(ids: List<Long>):  List<ClusterCrossRef> = dao.getByClusterIds(ids)
    suspend fun upsertClusterCrossRefs(crossRefs: List<ClusterCrossRef>) {
        dao.upsert(crossRefs)
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