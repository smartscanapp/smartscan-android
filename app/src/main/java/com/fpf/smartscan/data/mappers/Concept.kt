package com.fpf.smartscan.data.mappers

import com.fpf.smartscan.concepts.Concept
import com.fpf.smartscan.concepts.ConceptCrossRef
import com.fpf.smartscan.data.concepts.ConceptCrossRefEntity
import com.fpf.smartscan.data.concepts.ConceptEntity
import com.fpf.smartscan.data.concepts.ConceptWithCount

fun ConceptWithCount.toDomain(): Concept = Concept(
    id = concept.id,
    description = concept.description,
    updatedAt = concept.updatedAt,
    isPinned = concept.isPinned,
    size = count
)

fun Concept.toEntity(): ConceptEntity = ConceptEntity(
    id = id,
    description = description,
    isPinned = isPinned,
    updatedAt = updatedAt,
)

fun ConceptCrossRef.toEntity() = ConceptCrossRefEntity(
    conceptId=conceptId,
    mediaId = mediaId,
    mediaType = mediaType
)

fun ConceptCrossRefEntity.toDomain() = ConceptCrossRef(
    conceptId=conceptId,
    mediaId = mediaId,
    mediaType = mediaType
)