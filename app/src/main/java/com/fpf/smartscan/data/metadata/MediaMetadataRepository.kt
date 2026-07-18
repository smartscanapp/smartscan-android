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
    suspend fun insert(item: MediaMetadata) = dao.insert(listOf(item.toEntity()))

    suspend fun update(items: List<MediaMetadata>) = dao.update(items.map{it.toEntity()})
    suspend fun update(item: MediaMetadata) = dao.update(listOf(item.toEntity()))
    suspend fun getUnclusteredItemIds(): Map<Long, MediaType> = dao.getUnclusteredItemIds().associate { it.id to it.type }
    suspend fun getByIds(mediaIds: List<Long>, type: MediaType): List<MediaMetadata> = dao.getByIds(mediaIds, type).map{it.toDomain()}
    suspend fun getByType(type: MediaType): List<MediaMetadata> = dao.getByType(type).map{it.toDomain()}
    suspend fun getIdsByType(type: MediaType): List<Long> = dao.getIdsByType(type)
    suspend fun getByTag(tagId: Long, limit: Int, offset: Int, mediaType: MediaType?=null, startDate: Long? = null, endDate: Long? = null, ascending: Boolean = false): List<MediaMetadata> = if(ascending){
        dao.getByTagAsc(tagId, mediaType, limit = limit, offset=offset, startDate=startDate, endDate=endDate).map{it.toDomain()}
    }else{
        dao.getByTagDesc(tagId, mediaType, limit = limit, offset=offset, startDate=startDate, endDate=endDate).map{it.toDomain()}
    }
    suspend fun getByTag(tagId: Long, mediaType: MediaType?=null, startDate: Long? = null, endDate: Long? = null): List<MediaMetadata> = dao.getByTag(tagId, mediaType, startDate=startDate, endDate=endDate).map{it.toDomain()}
    suspend fun getByTagsWithoutDescription(tagIds: List<Long>, mediaType: MediaType): List<MediaMetadata> = dao.getByTagsWithoutDescription(tagIds, mediaType).map{it.toDomain()}
    suspend fun getByCluster(clusterId: Long, mediaType: MediaType?=null): List<MediaMetadata> = dao.getByCluster(clusterId, mediaType).map{it.toDomain()}
    suspend fun getByCluster(clusterId: Long, type: MediaType?=null, limit: Int, offset: Int, ascending: Boolean = false): List<MediaMetadata> = if(ascending){
        dao.getByClusterAsc(clusterId, type,limit=limit, offset=offset).map{it.toDomain()}
    }else{
        dao.getByClusterDesc(clusterId, type,limit=limit, offset=offset).map{it.toDomain()}
    }
    suspend fun getByClustersWithoutDescription(clusterIds: List<Long>, mediaType: MediaType? = null): List<MediaMetadata> = dao.getByClustersWithoutDescription(clusterIds, mediaType).map{it.toDomain()}

    suspend fun getByConceptSortedByDate(conceptId: Long,  minSimilarity: Float?= null, mediaType: MediaType? = null): List<MediaMetadata> = dao.getByConceptSortedByDate(conceptId, minSimilarity=minSimilarity, mediaType =mediaType).map{it.toDomain()}
    suspend fun getByConceptSortedByDate(conceptId: Long,  minSimilarity: Float?= null, mediaType: MediaType? = null, limit: Int, offset: Int, ascending: Boolean = false): List<MediaMetadata> = if(ascending){
        dao.getByConceptSortedByDateAsc(conceptId, minSimilarity=minSimilarity, mediaType =mediaType, limit=limit, offset=offset).map{it.toDomain()}
    }else{
        dao.getByConceptSortedByDateDesc(conceptId, minSimilarity=minSimilarity, mediaType =mediaType, limit=limit, offset=offset).map{it.toDomain()}
    }
    suspend fun getByConceptSortedBySimilarity(conceptId: Long, limit: Int, offset: Int, minSimilarity: Float?= null, mediaType: MediaType? = null, ascending: Boolean = false): List<MediaMetadata> = if(ascending){
        dao.getByConceptSortedBySimilarityAsc(conceptId, minSimilarity=minSimilarity, mediaType = mediaType, limit=limit, offset=offset).map{it.toDomain()}
    }else{
        dao.getByConceptSortedBySimilarityDesc(conceptId, minSimilarity=minSimilarity, mediaType = mediaType, limit=limit, offset=offset).map{it.toDomain()}
    }
    suspend fun getByConceptSortedBySimilarity(conceptId: Long, minSimilarity: Float?= null, mediaType: MediaType? = null): List<MediaMetadata> = dao.getByConceptSortedBySimilarity(conceptId, minSimilarity=minSimilarity, mediaType = mediaType).map{it.toDomain()}

    suspend fun deleteByMediaIds(ids: List<Long>, type: MediaType) = dao.deleteByIds(ids, type)

    suspend fun clear() = dao.clear()

    suspend fun emitEvent(event: MediaEvent) = _event.emit(event)

}