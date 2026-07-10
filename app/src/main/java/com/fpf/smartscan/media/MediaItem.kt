package com.fpf.smartscan.media

import android.net.Uri

data class MediaItem(
    val id: Long,
    val type: MediaType,
    val uri: Uri = mediaIdToUri(id, type),
    val description: String? = null
)
