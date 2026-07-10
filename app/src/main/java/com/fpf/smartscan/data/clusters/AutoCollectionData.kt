package com.fpf.smartscan.data.clusters

import com.fpf.smartscan.media.MediaType

data class AutoCollectionData(
    val clusterId: Long,
    val label: String?,
    val prototypeSize: Int,
    val thumbNailId: Long,
    val thumbNailType: MediaType
)