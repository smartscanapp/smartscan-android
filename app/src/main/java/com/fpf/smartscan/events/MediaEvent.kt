package com.fpf.smartscan.events

import com.fpf.smartscan.media.MediaType
import com.fpf.smartscansdk.core.embeddings.StoredEmbedding

enum class MediaEventType{
    EMBED_UPDATE
}
data class MediaEvent (
    val eventType: MediaEventType,
    val updatedEmbed: StoredEmbedding,
    val mediaType: MediaType
)