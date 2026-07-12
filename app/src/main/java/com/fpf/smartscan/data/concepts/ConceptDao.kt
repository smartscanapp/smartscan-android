package com.fpf.smartscan.data.concepts


import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ConceptDao {

    @Query("""
        SELECT c.*, COUNT(crossRef.mediaId) AS count
        FROM concept c
        LEFT JOIN concept_crossref crossRef ON c.id = crossRef.conceptId
        GROUP BY c.id
    """)
    fun getFlow(): Flow<List<ConceptWithCount>>

    @Query("""
        SELECT c.*, COUNT(crossRef.mediaId) AS count
        FROM concept c
        LEFT JOIN concept_crossref crossRef ON c.id = crossRef.conceptId
        GROUP BY c.id
    """)
    suspend fun get(): List<ConceptWithCount>

    @Query("""
        SELECT c.*, COUNT(crossRef.mediaId) AS count
        FROM concept c
        LEFT JOIN concept_crossref crossRef ON c.id = crossRef.conceptId
        WHERE c.id IN (:ids)
        GROUP BY c.id
    """)
    suspend fun get(ids: List<Long>): List<ConceptWithCount>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(concepts: List<ConceptEntity>): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(concepts: List<ConceptEntity>): List<Long>
    @Update
    suspend fun update(concepts: List<ConceptEntity>)
    @Delete
    suspend fun delete(concepts: List<ConceptEntity>)
    @Query("SELECT COUNT(*) FROM concept")
    suspend fun count(): Int
    @Query("DELETE FROM concept")
    suspend fun clear()
}