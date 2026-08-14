package com.fpf.smartscan.core.data.concepts


import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.fpf.smartscan.core.media.MediaType

@Dao
interface ConceptCrossRefDao {

    @Query("SELECT * FROM concept_crossref")
    suspend fun get(): List<ConceptCrossRefEntity>

    @Query("SELECT * FROM concept_crossref WHERE conceptId = :conceptId AND mediaId =:mediaId AND mediaType = :mediaType")
    suspend fun get(conceptId: Long, mediaId: Long, mediaType: MediaType): ConceptCrossRefEntity?

    @Query("SELECT * FROM concept_crossref WHERE conceptId in (:ids)")
    suspend fun getByConceptIds(ids: List<Long>): List<ConceptCrossRefEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(crossRefs: List<ConceptCrossRefEntity>)

    @Query("""
        UPDATE concept_crossref
        SET isHidden = :isHidden
        WHERE mediaId = :mediaId
          AND mediaType = :mediaType
          AND conceptId = :conceptId
    """)
    suspend fun setCrossRefHidden(mediaId: Long, mediaType: MediaType, conceptId: Long, isHidden: Boolean)

    @Query("DELETE FROM concept_crossref")
    suspend fun clear()

    @Query("SELECT COUNT(*) FROM concept_crossref")
    suspend fun count(): Int

    @Query("SELECT COUNT(*) FROM concept_crossref WHERE conceptId = :conceptId")
    suspend fun countByConceptId(conceptId: Long): Int

    @Delete
    suspend fun delete(crossrefs: List<ConceptCrossRefEntity>)


    @Transaction
    @Query("DELETE FROM concept_crossref WHERE mediaId =:mediaId AND mediaType =:mediaType ")
    suspend fun delete(mediaId: Long, mediaType: MediaType)
}