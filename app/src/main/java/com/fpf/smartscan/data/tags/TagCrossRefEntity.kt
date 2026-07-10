package com.fpf.smartscan.data.tags

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import com.fpf.smartscan.data.metadata.MediaMetadata
import com.fpf.smartscan.media.MediaType

@Entity(
    tableName = "tag_crossref",
    primaryKeys = ["mediaId", "mediaType", "tagId"],
    foreignKeys = [
        ForeignKey(
            entity = TagEntity::class,
            parentColumns = ["id"],
            childColumns = ["tagId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = MediaMetadata::class,
            parentColumns = ["id", "type"],
            childColumns = ["mediaId", "mediaType"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["tagId"]),
    ]
)
data class TagCrossRefEntity(
    val mediaId: Long,
    val mediaType: MediaType,
    val tagId: Long
)