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
    val type: CollectionType
): Parcelable{
    companion object {
        const val UNLABELLED_COLLECTION = "?"
    }
}