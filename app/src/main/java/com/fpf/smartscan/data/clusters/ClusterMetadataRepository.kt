package com.fpf.smartscan.data.clusters

import com.fpf.smartscan.data.mappers.toDomain
import com.fpf.smartscan.data.mappers.toEntity
import com.fpf.smartscan.media.MediaCollection
import com.fpf.smartscansdk.core.cluster.ClusterMetadata
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.collections.associate

class ClusterMetadataRepository(private val dao: ClusterMetadataDao) {
    suspend fun getAllMetadataAsMap(): Map<Long, ClusterMetadata> = dao.get().associate {
            it.clusterId to it.toDomain().second
        }

    fun getCollections(): Flow<List<MediaCollection>> = dao.getCollections().map{collections -> collections.map{it.toDomain()}}
    suspend fun getMetadata(ids: List<Long>): List<Pair<Long, ClusterMetadata>> = dao.get(ids).map{it.toDomain()}
    suspend fun getMetadata(id: Long): Pair<Long, ClusterMetadata>? = dao.get(listOf(id)).firstOrNull()?.toDomain()

    suspend fun count(minSize: Int = 1): Int = dao.count(minSize)
    suspend fun insertMetadata(metadataBatch: List<Pair<Long, ClusterMetadata>>) = dao.insert(metadataBatch.map{it.toEntity()})
    suspend fun insertMetadata(metadata: Pair<Long, ClusterMetadata>) = dao.insert(listOf(metadata.toEntity()))

    suspend fun updateMetadata(metadataBatch: List<Pair<Long, ClusterMetadata>>) = dao.update(metadataBatch.map{it.toEntity()})
    suspend fun updateMetadata(metadata: Pair<Long, ClusterMetadata>) = dao.update(listOf(metadata.toEntity()))

    suspend fun deleteMetadata(ids: List<Long>) = dao.delete(ids)
    suspend fun deleteMetadata(id: Long) = dao.delete(listOf(id))

    suspend fun clear() = dao.clear()
}