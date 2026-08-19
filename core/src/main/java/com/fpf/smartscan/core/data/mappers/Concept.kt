package com.fpf.smartscan.core.data.mappers

import com.fpf.smartscan.core.concepts.Concept
import com.fpf.smartscan.core.concepts.ConceptCrossRef
import com.fpf.smartscan.core.concepts.NewConcept
import com.fpf.smartscan.core.data.concepts.ConceptCrossRefEntity
import com.fpf.smartscan.core.data.concepts.ConceptEntity
import com.fpf.smartscan.core.data.concepts.ConceptWithCount

fun ConceptWithCount.toDomain(): Concept = Concept(
    id = concept.id,
    description = concept.description,
    updatedAt = concept.updatedAt,
    isPinned = concept.isPinned,
    size = count
)

fun Concept.toEntity(): ConceptEntity =
    ConceptEntity(
        id = id,
        description = description,
        isPinned = isPinned,
        updatedAt = updatedAt,
    )

fun NewConcept.toEntity(): ConceptEntity =
    ConceptEntity(
        description = description,
        isPinned = isPinned,
        updatedAt = updatedAt,
    )
fun ConceptCrossRef.toEntity() =
   ConceptCrossRefEntity(
        conceptId = conceptId,
        mediaId = mediaId,
        mediaType = mediaType,
        similarity = similarity,
       isHidden = isHidden
    )

fun ConceptCrossRefEntity.toDomain() = ConceptCrossRef(
    conceptId=conceptId,
    mediaId = mediaId,
    mediaType = mediaType,
    similarity = similarity,
    isHidden = isHidden
)