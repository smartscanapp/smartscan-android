package com.fpf.smartscan.core.data.concepts

import com.fpf.smartscan.core.concepts.ConceptCrossRef
import com.fpf.smartscan.core.data.mappers.toDomain
import com.fpf.smartscan.core.data.mappers.toEntity
import com.fpf.smartscan.core.media.MediaType

class ConceptCrossRefRepository(private val dao: ConceptCrossRefDao) {
    suspend fun getAllCrossRefs(): List<ConceptCrossRef> = dao.get().map{it.toDomain()}
    suspend fun getByConceptIds(ids: List<Long>):  List<ConceptCrossRef> = dao.getByConceptIds(ids).map{it.toDomain()}
    suspend fun setCrossRefHidden(mediaId: Long, mediaType: MediaType, conceptId: Long, isHidden: Boolean) = dao.setCrossRefHidden(mediaId, mediaType, conceptId, isHidden)
    suspend fun getCrossRef(conceptId: Long, mediaId: Long, mediaType: MediaType) = dao.get(conceptId, mediaId, mediaType )

    suspend fun insertConceptCrossRefs(crossRefs: List<ConceptCrossRef>) = dao.insert(crossRefs.map { it.toEntity() })
    suspend fun clear() = dao.clear()
    suspend fun count() = dao.count()
    suspend fun count(clusterId: Long) = dao.countByConceptId(clusterId)
    suspend fun delete(crossrefs: List<ConceptCrossRef>) = dao.delete(crossrefs.map{it.toEntity()})
    suspend fun delete(mediaId: Long, type: MediaType) = dao.delete(mediaId, type)

}