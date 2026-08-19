package com.fpf.smartscan.core.media

import android.net.Uri
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

enum class CollectionType {
    CLUSTER,
    TAG
}

@Parcelize
data class MediaCollection (
    val id: Long,
    val name: String,
    val thumbNail: Uri,
    val size: Int,
    val imageCount: Int,
    val duplicateImageCount: Int,
    val type: CollectionType
): Parcelable{
    val videoCount: Int
        get() = (size - imageCount).coerceAtLeast(0)

    val duplicateVideoCount: Int
        get() = 0 // No dedupe for videos yet. May add in the future
    companion object {
        const val UNLABELLED_COLLECTION = "?"
    }
}