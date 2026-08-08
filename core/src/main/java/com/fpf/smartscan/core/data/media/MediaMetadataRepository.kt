package com.fpf.smartscan.core.data.media

import com.fpf.smartscan.core.data.mappers.toDomain
import com.fpf.smartscan.core.data.mappers.toEntity
import com.fpf.smartscan.core.media.MediaMetadata
import com.fpf.smartscan.core.media.MediaType

class MediaMetadataRepository(
    private val dao: MediaMetadataDao
) {

    suspend fun insert(items: List<MediaMetadata>) = dao.insert(items.map{it.toEntity()})
    suspend fun insert(item: MediaMetadata) = dao.insert(listOf(item.toEntity()))

    suspend fun update(items: List<MediaMetadata>) = dao.update(items.map{it.toEntity()})
    suspend fun update(item: MediaMetadata) = dao.update(listOf(item.toEntity()))
    suspend fun getUnclusteredItemIds(): Map<Long, MediaType> = dao.getUnclusteredItemIds().associate { it.id to it.type }
    suspend fun getByIds(mediaIds: List<Long>, type: MediaType, isTrashed: Boolean = false, isDuplicate: Boolean? = null): List<MediaMetadata> = dao.getByIds(mediaIds, type, isTrashed=isTrashed, isDuplicate=isDuplicate).map{it.toDomain()}
    suspend fun get(type: MediaType? = null, isTrashed: Boolean = false, isDuplicate: Boolean? = null): List<MediaMetadata> = dao.get(type, isTrashed=isTrashed, isDuplicate=isDuplicate).map{it.toDomain()}
    suspend fun getIds(type: MediaType? = null, isTrashed: Boolean = false, isDuplicate: Boolean? = null): List<Long> = dao.getIds(type, isTrashed=isTrashed, isDuplicate=isDuplicate)
    suspend fun countDuplicatesByIds(ids: List<Long>, mediaType: MediaType): Int = dao.countDuplicatesByIds(ids, mediaType)

    // TAGS
    suspend fun getByTag(tagId: Long, limit: Int, offset: Int, mediaType: MediaType?=null, startDate: Long? = null, endDate: Long? = null, ascending: Boolean = false, isDuplicate: Boolean? = null): List<MediaMetadata> = if(ascending){
        dao.getByTagAsc(tagId, mediaType=mediaType, limit = limit, offset=offset, startDate=startDate, endDate=endDate, isDuplicate=isDuplicate).map{it.toDomain()}
    }else{
        dao.getByTagDesc(tagId, mediaType=mediaType, limit = limit, offset=offset, startDate=startDate, endDate=endDate, isDuplicate=isDuplicate).map{it.toDomain()}
    }
    suspend fun getByTag(tagId: Long, mediaType: MediaType?=null, startDate: Long? = null, endDate: Long? = null, isDuplicate: Boolean? = null): List<MediaMetadata> = dao.getByTag(tagId, mediaType, startDate=startDate, endDate=endDate, isDuplicate=isDuplicate).map{it.toDomain()}
    suspend fun getByTagsWithoutDescription(tagIds: List<Long>, mediaType: MediaType): List<MediaMetadata> = dao.getByTagsWithoutDescription(tagIds, mediaType).map{it.toDomain()}
    suspend fun countDuplicatesInTag(tagId: Long): Int = dao.countDuplicatesInTag(tagId)
    // CLUSTER
    suspend fun getByCluster(clusterId: Long, mediaType: MediaType?=null, isDuplicate: Boolean? = null): List<MediaMetadata> = dao.getByCluster(clusterId, mediaType, isDuplicate).map{it.toDomain()}
    suspend fun getByCluster(clusterId: Long, type: MediaType?=null, limit: Int, offset: Int, ascending: Boolean = false, isDuplicate: Boolean? = null): List<MediaMetadata> = if(ascending){
        dao.getByClusterAsc(clusterId, mediaType=type,limit=limit, offset=offset, isDuplicate=isDuplicate).map{it.toDomain()}
    }else{
        dao.getByClusterDesc(clusterId, mediaType=type,limit=limit, offset=offset, isDuplicate=isDuplicate).map{it.toDomain()}
    }
    suspend fun getByClustersWithoutDescription(clusterIds: List<Long>, mediaType: MediaType? = null): List<MediaMetadata> = dao.getByClustersWithoutDescription(clusterIds, mediaType).map{it.toDomain()}
    suspend fun countDuplicatesInCluster(clusterId: Long): Int = dao.countDuplicatesInCluster(clusterId)

    // CONCEPTS
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

    suspend fun markDuplicates(ids: List<Long>, mediaType: MediaType) = dao.markDuplicates(ids, mediaType)
    suspend fun deleteByMediaIds(ids: List<Long>, type: MediaType) = dao.deleteByIds(ids, type)

    suspend fun clear() = dao.clear()

}