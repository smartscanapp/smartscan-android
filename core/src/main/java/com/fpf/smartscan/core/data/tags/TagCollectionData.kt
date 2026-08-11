package com.fpf.smartscan.core.data.tags

import com.fpf.smartscan.core.media.MediaType

data class TagCollectionData(
    val tagId: Long,
    val name: String,
    val size: Int,
    val imageCount: Int,
    val duplicateImageCount: Int,
    val thumbNailId: Long,
    val thumbNailType: MediaType
)