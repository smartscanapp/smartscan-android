package com.fpf.smartscan.data.clusters

import com.fpf.smartscansdk.core.cluster.ClusterMetadata
import kotlinx.coroutines.flow.Flow
import kotlin.collections.associate

class ClusterMetadataRepository(private val dao: ClusterMetadataDao) {
    suspend fun getAllMetadataAsMap(): Map<Long, ClusterMetadata> = dao.get().associate {
            it.clusterId to it.toMetadata()
        }

    fun getCollections(): Flow<List<AutoCollectionData>> = dao.getCollections()
    suspend fun getMetadata(ids: List<Long>): List<ClusterMetadataEntity> = dao.get(ids)
    suspend fun getMetadata(id: Long): ClusterMetadataEntity? = dao.get(listOf(id)).firstOrNull()

    suspend fun count(minSize: Int = 1): Int = dao.count(minSize)
    suspend fun insertMetadata(metadataBatch: List<ClusterMetadataEntity>) = dao.insert(metadataBatch)
    suspend fun insertMetadata(metadata: ClusterMetadataEntity) = dao.insert(listOf(metadata))

    suspend fun updateMetadata(metadataBatch: List<ClusterMetadataEntity>) = dao.update(metadataBatch)
    suspend fun updateMetadata(metadata: ClusterMetadataEntity) = dao.update(listOf(metadata))

    suspend fun deleteMetadata(ids: List<Long>) = dao.delete(ids)
    suspend fun deleteMetadata(id: Long) = dao.delete(listOf(id))

    suspend fun clear() = dao.clear()
}