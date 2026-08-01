package com.fpf.smartscan.core.cluster

data class StoredClusterMetadata (
    val clusterId: Long,
    val prototypeSize: Int,
    val meanSimilarity: Float = 0f,
    val stdSimilarity: Float = 0f,
    val label: String? = null,
)