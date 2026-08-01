package com.fpf.smartscan.core.data.media


import androidx.room.Entity
import androidx.room.Index
import com.fpf.smartscan.core.media.MediaType

@Entity(
    primaryKeys = ["id", "type"],
    tableName = "media_metadata",
    indices = [
        Index(value = ["dateAdded"]),
        Index(value = ["type", "dateAdded"])
    ])
data class MediaMetadataEntity(
    val id: Long,
    val type: MediaType,
    val dateAdded: Long,
    val description: String? = null
)
