package com.fpf.smartscan.data.concepts

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import com.fpf.smartscan.data.metadata.MediaMetadataEntity
import com.fpf.smartscan.media.MediaType

@Entity(
    tableName = "concept_crossref",
    primaryKeys = ["mediaId", "mediaType", "conceptId"],
    foreignKeys = [
        ForeignKey(
            entity = ConceptEntity::class,
            parentColumns = ["id"],
            childColumns = ["conceptId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = MediaMetadataEntity::class,
            parentColumns = ["id", "type"],
            childColumns =  ["mediaId", "mediaType"],
            onDelete = ForeignKey.CASCADE
        ),
    ],
    indices = [
        Index(value = ["conceptId"])
    ]
)
data class ConceptCrossRefEntity (
    val mediaId: Long,
    val conceptId: Long,
    val mediaType: MediaType
)