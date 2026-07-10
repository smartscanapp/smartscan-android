package com.fpf.smartscan.data.mappers

import com.fpf.smartscan.cluster.ClusterCrossRef
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

fun ClusterMetadataEntity.toDomain(): Pair<Long, ClusterMetadata> = Pair(clusterId,
    ClusterMetadata(
        prototypeSize = prototypeSize,
        label=label,
        meanSimilarity = meanSimilarity,
        stdSimilarity = stdSimilarity
    )
)

fun Pair<Long, ClusterMetadata>.toEntity(): ClusterMetadataEntity = ClusterMetadataEntity(
    clusterId = first,
    label = second.label,
    meanSimilarity = second.meanSimilarity,
    stdSimilarity = second.stdSimilarity,
    prototypeSize = second.prototypeSize
)