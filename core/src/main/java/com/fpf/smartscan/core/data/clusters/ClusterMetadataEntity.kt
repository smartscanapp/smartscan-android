package com.fpf.smartscan.core.data.clusters

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.fpf.smartscan.core.data.MediaTypeConverter

@Entity(
    tableName = "cluster_metadata",
    indices = [
        Index(value = ["label"], unique = true),
    ])
@TypeConverters(MediaTypeConverter::class)
data class ClusterMetadataEntity (
    @PrimaryKey
    val clusterId: Long,
    val prototypeSize: Int,
    val meanSimilarity: Float = 0f,
    val stdSimilarity: Float = 0f,
    val label: String? = null,
    )