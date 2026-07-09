package com.fpf.smartscan.data.concepts

import com.fpf.smartscan.concepts.ConceptCrossRef
import com.fpf.smartscan.data.mappers.toDomain
import com.fpf.smartscan.data.mappers.toEntity

class ConceptCrossRefRepository(private val dao: ConceptCrossRefDao) {
    suspend fun getAllCrossRefs(): List<ConceptCrossRef> = dao.get().map{it.toDomain()}
    suspend fun getByConceptIds(ids: List<Long>):  List<ConceptCrossRef> = dao.getByConceptIds(ids).map{it.toDomain()}
    suspend fun upsertConceptCrossRefs(crossRefs: List<ConceptCrossRef>) = dao.upsert(crossRefs.map { it.toEntity() })
    suspend fun clear() = dao.clear()
    suspend fun count() = dao.count()
    suspend fun count(clusterId: Long) = dao.countByConceptId(clusterId)
}