package com.fpf.smartscan.core.cluster

import com.fpf.smartscan.core.media.MediaType

data class ClusterCrossRef(
    val mediaId: Long,
    val mediaType: MediaType,
    val clusterId: Long
)