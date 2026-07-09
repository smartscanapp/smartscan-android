package com.fpf.smartscan.data.concepts


import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ConceptCrossRefDao {

    @Query("SELECT * FROM concept_crossref")
    suspend fun get(): List<ConceptCrossRefEntity>

    @Query("SELECT * FROM concept_crossref WHERE conceptId in (:ids)")
    suspend fun getByConceptIds(ids: List<Long>): List<ConceptCrossRefEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(crossRefs: List<ConceptCrossRefEntity>)

    @Query("DELETE FROM concept_crossref")
    suspend fun clear()

    @Query("SELECT COUNT(*) FROM concept_crossref")
    suspend fun count(): Int

    @Query("SELECT COUNT(*) FROM concept_crossref WHERE conceptId = :conceptId")
    suspend fun countByConceptId(conceptId: Long): Int
}