package com.fpf.smartscan.data.clusters

import com.fpf.smartscan.cluster.StoredClusterMetadata
import com.fpf.smartscan.data.mappers.toDomain
import com.fpf.smartscan.data.mappers.toEntity
import com.fpf.smartscan.media.MediaCollection
import com.fpf.smartscansdk.core.cluster.ClusterMetadata
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.collections.associate

class ClusterMetadataRepository(private val dao: ClusterMetadataDao) {
    suspend fun getAllMetadataAsMap(): Map<Long, StoredClusterMetadata> = dao.get().associate {
            it.clusterId to it.toDomain()
        }

    fun getCollections(): Flow<List<MediaCollection>> = dao.getCollections().map{collections -> collections.map{it.toDomain()}}
    suspend fun getCollections(clusterIds: List<Long>): List<MediaCollection> = dao.getCollections(clusterIds).map{ it.toDomain()}
    suspend fun getMetadata(ids: List<Long>): List<StoredClusterMetadata> = dao.get(ids).map{it.toDomain()}
    suspend fun getMetadata(id: Long): StoredClusterMetadata? = dao.get(listOf(id)).firstOrNull()?.toDomain()

    suspend fun count(minSize: Int = 1): Int = dao.count(minSize)
    suspend fun insertMetadata(metadataBatch: List<StoredClusterMetadata>) = dao.insert(metadataBatch.map{it.toEntity()})
    suspend fun insertMetadata(metadata: StoredClusterMetadata) = dao.insert(listOf(metadata.toEntity()))

    suspend fun updateMetadata(metadataBatch: List<StoredClusterMetadata>) = dao.update(metadataBatch.map{it.toEntity()})
    suspend fun updateMetadata(metadata: StoredClusterMetadata) = dao.update(listOf(metadata.toEntity()))

    suspend fun deleteMetadata(ids: List<Long>) = dao.delete(ids)
    suspend fun deleteMetadata(id: Long) = dao.delete(listOf(id))

    suspend fun clear() = dao.clear()
}