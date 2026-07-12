package com.fpf.smartscan.media

import android.net.Uri

data class MediaItem(
    val id: Long,
    val type: MediaType,
    val description: String? = null
) {
    val uri: Uri
        get() = MediaStoreHelper.mediaIdToUri(id, type)
}