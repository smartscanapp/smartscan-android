package com.fpf.smartscan.data.concepts

import com.fpf.smartscan.concepts.Concept
import com.fpf.smartscan.data.mappers.toDomain
import com.fpf.smartscan.data.mappers.toEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ConceptRepository(private val dao: ConceptDao) {
    fun getConceptsFlow(): Flow<List<Concept>> = dao.getFlow().map{ conceptWithCount -> conceptWithCount.map{it.toDomain()}}
    suspend fun getConcepts(): List<Concept> = dao.get().map{it.toDomain()}
    suspend fun getConcepts(ids: List<Long>): List<Concept> = dao.get(ids).map{it.toDomain()}
    suspend fun getConcept(id: Long): Concept? = dao.get(listOf(id)).firstOrNull()?.toDomain()
    suspend fun insertConcepts(concepts: List<Concept>) = dao.insert(concepts.map{it.toEntity()})
    suspend fun insertConcept(concept: Concept) = dao.insert(listOf(concept.toEntity()))
    suspend fun updateConcepts(concepts: List<Concept>) = dao.update(concepts.map{it.toEntity()})
    suspend fun updateConcept(concept: Concept) = dao.update(listOf(concept.toEntity()))
    suspend fun deleteConceptsByIds(ids: List<Long>) = dao.deleteByIds(ids)
    suspend fun deleteConcepts(concepts: List<Concept>) = dao.delete(concepts.map { it.toEntity() })
    suspend fun count(): Int = dao.count()
    suspend fun clear() = dao.clear()
}