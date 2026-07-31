package com.fpf.smartscan.core.data.concepts

import com.fpf.smartscan.core.concepts.Concept
import com.fpf.smartscan.core.data.mappers.toDomain
import com.fpf.smartscan.core.data.mappers.toEntity
import com.fpf.smartscan.core.media.MediaType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ConceptRepository(private val dao: ConceptDao) {
    fun getConceptsFlow(): Flow<List<Concept>> = dao.getFlow().map{ conceptWithCount -> conceptWithCount.map{it.toDomain()}}
    suspend fun getConcepts(): List<Concept> = dao.get().map{it.toDomain()}
    suspend fun getConcepts(ids: List<Long>): List<Concept> = dao.get(ids).map{it.toDomain()}
    suspend fun getConcept(id: Long): Concept? = dao.get(listOf(id)).firstOrNull()?.toDomain()

    suspend fun getLinkedConceptIds(mediaId: Long, mediaType: MediaType) = dao.getLinkedIds(mediaId,mediaType)
    suspend fun getUnlinkedConceptIds(mediaId: Long, mediaType: MediaType) = dao.getUnlinkedIds(mediaId,mediaType)

    suspend fun insertConcepts(concepts: List<Concept>) = dao.insert(concepts.map{it.toEntity()})
    suspend fun insertConcept(concept: Concept) = dao.insert(listOf(concept.toEntity()))
    suspend fun updateConcepts(concepts: List<Concept>) = dao.update(concepts.map{it.toEntity()})
    suspend fun updateConcept(concept: Concept) = dao.update(listOf(concept.toEntity()))
    suspend fun upsertConcepts(concepts: List<Concept>) = dao.upsert(concepts.map{it.toEntity()})
    suspend fun upsertConcept(concept: Concept) = dao.upsert(listOf(concept.toEntity()))
    suspend fun deleteConcepts(concepts: List<Concept>) = dao.delete(concepts.map { it.toEntity() })
    suspend fun count(): Int = dao.count()
    suspend fun clear() = dao.clear()
}