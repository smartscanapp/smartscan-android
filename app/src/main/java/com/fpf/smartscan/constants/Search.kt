package com.fpf.smartscan.constants

import com.fpf.smartscan.media.MediaType

val mediaTypeOptions = mapOf(
    MediaType.IMAGE to "Images",
    MediaType.VIDEO to "Videos",
)

object EmbeddingStoresFiles{
    const val IMAGE: String = "image_index.bin"
    const val VIDEO: String = "video_index.bin"

    const val MEDIA_CLUSTER: String = "media_cluster_index.bin"
    const val IMAGE_CLUSTER: String = "image_cluster_index.bin"

    const val VIDEO_CLUSTER: String = "video_cluster_index.bin"
    const val TAGS: String = "tags_store.bin"
}