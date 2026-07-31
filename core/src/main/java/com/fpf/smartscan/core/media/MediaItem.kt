package com.fpf.smartscan.core.media

import android.net.Uri

data class MediaItem(
    val id: Long,
    val type: MediaType,
    val dateAdded: Long,
    val description: String? = null
) {
    val uri: Uri
        get() = MediaStoreHelper.mediaIdToUri(id, type)
}