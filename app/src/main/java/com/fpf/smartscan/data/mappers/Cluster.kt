package com.fpf.smartscan.data.mappers

import com.fpf.smartscan.cluster.ClusterCrossRef
import com.fpf.smartscan.cluster.StoredClusterMetadata
import com.fpf.smartscan.data.clusters.ClusterCrossRefEntity
import com.fpf.smartscan.data.clusters.ClusterMetadataEntity
import com.fpf.smartscansdk.core.cluster.ClusterMetadata


fun ClusterCrossRef.toEntity(): ClusterCrossRefEntity = ClusterCrossRefEntity(
    mediaId = mediaId,
    clusterId = clusterId,
    mediaType = mediaType
)

fun ClusterCrossRefEntity.toDomain(): ClusterCrossRef = ClusterCrossRef(
    mediaId = mediaId,
    clusterId = clusterId,
    mediaType = mediaType
)

fun ClusterMetadataEntity.toDomain(): StoredClusterMetadata = StoredClusterMetadata(
    clusterId = clusterId,
    label = label,
    meanSimilarity = meanSimilarity,
    stdSimilarity = stdSimilarity,
    prototypeSize = prototypeSize
)

fun StoredClusterMetadata.toEntity(): ClusterMetadataEntity = ClusterMetadataEntity(
    clusterId = clusterId,
    label = label,
    meanSimilarity = meanSimilarity,
    stdSimilarity = stdSimilarity,
    prototypeSize = prototypeSize
)

fun StoredClusterMetadata.toIncrementalClusterMetadata(): ClusterMetadata = ClusterMetadata(
    label = label,
    meanSimilarity = meanSimilarity,
    stdSimilarity = stdSimilarity,
    prototypeSize = prototypeSize
)