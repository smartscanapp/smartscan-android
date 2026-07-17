package com.fpf.smartscan.data.metadata

import com.fpf.smartscan.data.mappers.toDomain
import com.fpf.smartscan.data.mappers.toEntity
import com.fpf.smartscan.events.MediaEvent
import com.fpf.smartscan.media.MediaMetadata
import com.fpf.smartscan.media.MediaType
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class MediaMetadataRepository(
    private val dao: MediaMetadataDao
) {

    private val _event = MutableSharedFlow<MediaEvent>()
    val event = _event.asSharedFlow()

    suspend fun insert(items: List<MediaMetadata>) = dao.insert(items.map{it.toEntity()})

    suspend fun update(items: List<MediaMetadata>) = dao.update(items.map{it.toEntity()})
    suspend fun getUnclusteredItemIds(): Map<Long, MediaType> = dao.getUnclusteredItemIds().associate { it.id to it.type }

    suspend fun getByIds(mediaIds: List<Long>, type: MediaType): List<MediaMetadata> = dao.getByIds(mediaIds, type).map{it.toDomain()}
    suspend fun getByType(type: MediaType): List<MediaMetadata> = dao.getByType(type).map{it.toDomain()}
    suspend fun getIdsByType(type: MediaType): List<Long> = dao.getIdsByType(type)
    suspend fun getByTag(tagId: Long): List<MediaMetadata> = dao.getByTag(tagId).map{it.toDomain()}
    suspend fun getByTag(tagId: Long, limit: Int, offset: Int): List<MediaMetadata> = dao.getByTag(tagId, limit=limit, offset=offset).map{it.toDomain()}
    suspend fun getByTag(tagId: Long, type: MediaType, limit: Int, offset: Int): List<MediaMetadata> = dao.getByTag(tagId, type, limit=limit, offset=offset).map{it.toDomain()}
    suspend fun getByTag(tagId: Long, type: MediaType): List<MediaMetadata> = dao.getByTag(tagId, type).map{it.toDomain()}
    suspend fun getByTag(tagId: Long, type: MediaType, startDate: Long?, endDate: Long?): List<MediaMetadata> = dao.getByTag(tagId, type, startDate, endDate).map{it.toDomain()}

    suspend fun getByTagsWithoutDescription(tagIds: List<Long>, mediaType: MediaType): List<MediaMetadata> = dao.getByTagsWithoutDescription(tagIds, mediaType).map{it.toDomain()}
    suspend fun getByCluster(clusterId: Long, limit: Int, offset: Int): List<MediaMetadata> = dao.getByCluster(clusterId, limit=limit, offset=offset).map{it.toDomain()}
    suspend fun getByCluster(clusterId: Long): List<MediaMetadata> = dao.getByCluster(clusterId).map{it.toDomain()}
    suspend fun getByCluster(clusterId: Long, type: MediaType, limit: Int, offset: Int): List<MediaMetadata> = dao.getByCluster(clusterId, type,limit=limit, offset=offset).map{it.toDomain()}
    suspend fun getByClustersWithoutDescription(clusterIds: List<Long>, mediaType: MediaType): List<MediaMetadata> = dao.getByClustersWithoutDescription(clusterIds, mediaType).map{it.toDomain()}

    suspend fun getByConcept(conceptId: Long, limit: Int, offset: Int): List<MediaMetadata> = dao.getByConcept(conceptId, limit=limit, offset=offset).map{it.toDomain()}
    suspend fun getByConcept(conceptId: Long): List<MediaMetadata> = dao.getByConcept(conceptId).map{it.toDomain()}
    suspend fun getByConcept(conceptId: Long, type: MediaType, limit: Int, offset: Int): List<MediaMetadata> = dao.getByConcept(conceptId, type, limit=limit, offset=offset).map{it.toDomain()}
    suspend fun getByConceptSortedBySimilarity(conceptId: Long, type: MediaType, limit: Int, offset: Int): List<MediaMetadata> = dao.getByConceptSortedBySimilarity(conceptId, type, limit=limit, offset=offset).map{it.toDomain()}
    suspend fun getByConceptSortedBySimilarity(conceptId: Long, type: MediaType): List<MediaMetadata> = dao.getByConceptSortedBySimilarity(conceptId, type).map{it.toDomain()}
    suspend fun getByConceptWithMinimumSimilarity(conceptId: Long, type: MediaType, minSimilarity: Float, limit: Int, offset: Int): List<MediaMetadata> = dao.getByConceptWithMinimumSimilarity(conceptId, type, minSimilarity=minSimilarity, limit=limit, offset=offset).map{it.toDomain()}
    suspend fun getByConceptWithMinimumSimilarity(conceptId: Long, type: MediaType): List<MediaMetadata> = dao.getByConceptSortedBySimilarity(conceptId, type).map{it.toDomain()}

    suspend fun deleteByMediaIds(ids: List<Long>, type: MediaType) = dao.deleteByIds(ids, type)

    suspend fun clear() = dao.clear()

    suspend fun emitEvent(event: MediaEvent) = _event.emit(event)

}