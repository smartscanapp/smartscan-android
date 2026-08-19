package com.fpf.smartscan.core.data.clusters

import com.fpf.smartscan.core.media.MediaType

data class AutoCollectionData(
    val clusterId: Long,
    val label: String?,
    val prototypeSize: Int,
    val imageCount: Int,
    val duplicateImageCount: Int,
    val thumbNailId: Long,
    val thumbNailType: MediaType,
)