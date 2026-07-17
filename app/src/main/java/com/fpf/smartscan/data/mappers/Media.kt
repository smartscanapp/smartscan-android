package com.fpf.smartscan.data.mappers

import com.fpf.smartscan.data.metadata.MediaMetadataEntity
import com.fpf.smartscan.media.MediaItem
import com.fpf.smartscan.media.MediaMetadata

fun MediaMetadataEntity.toDomain(): MediaMetadata = MediaMetadata(
    id=id,
    type = type,
    dateAdded = dateAdded,
    description=description
)

fun MediaMetadata.toEntity(): MediaMetadataEntity = MediaMetadataEntity(
    id=id,
    type = type,
    dateAdded = dateAdded,
    description=description
)

fun MediaMetadata.toItem(): MediaItem = MediaItem(
    id=id,
    type=type,
    dateAdded=dateAdded,
    description=description
)

fun MediaItem.toMetadata(): MediaMetadata = MediaMetadata(
    id=id,
    type=type,
    dateAdded=dateAdded,
    description=description
)

