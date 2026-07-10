package com.fpf.smartscan.cluster

import com.fpf.smartscan.media.MediaType

data class ClusterCrossRef(
    val mediaId: Long,
    val mediaType: MediaType,
    val clusterId: Long
)