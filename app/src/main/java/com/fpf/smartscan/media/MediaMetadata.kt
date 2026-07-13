package com.fpf.smartscan.media

data class MediaMetadata(
    val id: Long,
    val type: MediaType,
    val dateAdded: Long,
    val description: String? = null
)

fun MediaMetadata.toItem(): MediaItem = MediaItem(
    id=id,
    type=type,
    dateAdded=dateAdded,
    description=description
)