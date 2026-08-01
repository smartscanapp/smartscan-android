package com.fpf.smartscan.core.data.mappers

import com.fpf.smartscan.core.data.tags.TagCrossRefEntity
import com.fpf.smartscan.core.data.tags.TagEntity
import com.fpf.smartscan.core.tag.NewTag
import com.fpf.smartscan.core.tag.Tag
import com.fpf.smartscan.core.tag.TagCrossRef

fun TagCrossRef.toEntity(): TagCrossRefEntity =
    TagCrossRefEntity(
        tagId = tagId,
        mediaId = mediaId,
        mediaType = mediaType
    )

fun TagCrossRefEntity.toDomain(): TagCrossRef = TagCrossRef(
    tagId=tagId,
    mediaId = mediaId,
    mediaType = mediaType
)

fun Tag.toEntity(): TagEntity =
    TagEntity(
        id = id,
        name = name,
        lastUsedAt = lastUsedAt
    )

fun TagEntity.toDomain(): Tag = Tag(
    id = id,
    name=name,
    lastUsedAt=lastUsedAt
)

fun NewTag.toEntity(): TagEntity =
    TagEntity(
        name = name,
    )