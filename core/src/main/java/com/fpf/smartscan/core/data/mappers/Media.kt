package com.fpf.smartscan.core.data.mappers

import com.fpf.smartscan.core.data.media.HideableMediaEntity
import com.fpf.smartscan.core.data.media.MediaMetadataEntity
import com.fpf.smartscan.core.media.MediaItem
import com.fpf.smartscan.core.media.MediaMetadata

fun MediaMetadataEntity.toDomain(): MediaMetadata = MediaMetadata(
    id=id,
    type = type,
    dateAdded = dateAdded,
    description=description
)

fun MediaMetadata.toEntity(): MediaMetadataEntity =
    MediaMetadataEntity(
        id = id,
        type = type,
        dateAdded = dateAdded,
        description = description,
        isTrashed = isTrashed,
        isDuplicate = isDuplicate

    )

fun MediaMetadata.toItem(): MediaItem = MediaItem(
    id=id,
    type=type,
    dateAdded=dateAdded,
    description=description,
    isTrashed = isTrashed,
    isDuplicate = isDuplicate
)

fun MediaItem.toMetadata(): MediaMetadata = MediaMetadata(
    id=id,
    type=type,
    dateAdded=dateAdded,
    description=description,
    isTrashed = isTrashed,
    isDuplicate = isDuplicate
)

fun HideableMediaEntity.toDomain(): MediaMetadata = MediaMetadata(
    id=media.id,
    type=media.type,
    dateAdded=media.dateAdded,
    description=media.description,
    isTrashed = media.isTrashed,
    isDuplicate =media.isDuplicate,
    isHidden = isHidden
)
