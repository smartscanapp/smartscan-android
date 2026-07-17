package com.fpf.smartscan.data.tags

import com.fpf.smartscan.media.MediaType

data class TagCollectionData(
    val tagId: Long,
    val name: String,
    val size: Int,
    val thumbNailId: Long,
    val thumbNailType: MediaType
)