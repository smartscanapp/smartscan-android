package com.fpf.smartscan.data.clusters

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import com.fpf.smartscan.data.metadata.MediaMetadataEntity
import com.fpf.smartscan.media.MediaType

@Entity(
    tableName = "media_cluster_crossref",
    primaryKeys = ["mediaId", "mediaType"],
    foreignKeys = [
        ForeignKey(
            entity = ClusterMetadataEntity::class,
            parentColumns = ["clusterId"],
            childColumns = ["clusterId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = MediaMetadataEntity::class,
            parentColumns = ["id", "type"],
            childColumns = ["mediaId", "mediaType"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["clusterId"])
    ]
)
data class ClusterCrossRefEntity(
    val mediaId: Long,
    val mediaType: MediaType,
    val clusterId: Long
)