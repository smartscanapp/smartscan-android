package com.fpf.smartscan.core.media

data class MediaMetadata(
    val id: Long,
    val type: MediaType,
    val dateAdded: Long,
    val description: String? = null,
    val isDuplicate: Boolean = false,
    val isTrashed: Boolean = false,
    val isHidden: Boolean = false
)