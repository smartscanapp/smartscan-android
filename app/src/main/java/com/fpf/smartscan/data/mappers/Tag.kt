package com.fpf.smartscan.data.mappers

import com.fpf.smartscan.data.tags.TagCrossRefEntity
import com.fpf.smartscan.data.tags.TagEntity
import com.fpf.smartscan.tag.Tag
import com.fpf.smartscan.tag.TagCrossRef

fun TagCrossRef.toEntity(): TagCrossRefEntity = TagCrossRefEntity(
    tagId=tagId,
    mediaId = mediaId,
    mediaType = mediaType
)

fun TagCrossRefEntity.toDomain(): TagCrossRef = TagCrossRef(
    tagId=tagId,
    mediaId = mediaId,
    mediaType = mediaType
)

fun Tag.toEntity(): TagEntity = TagEntity(
    id = id,
    name=name,
    lastUsedAt=lastUsedAt
)

fun TagEntity.toDomain(): Tag = Tag(
    id = id,
    name=name,
    lastUsedAt=lastUsedAt
)