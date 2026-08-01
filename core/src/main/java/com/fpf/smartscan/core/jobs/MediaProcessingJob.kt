package com.fpf.smartscan.core.jobs

import com.fpf.smartscan.core.media.MediaItem
import com.fpf.smartscan.core.media.MediaType
import com.fpf.smartscansdk.core.embeddings.StoredEmbedding

sealed interface MediaProcessingJob{
    data class UpdateConceptLinks(val updatedEmbed: StoredEmbedding, val mediaType: MediaType): MediaProcessingJob
    data class UpdateDescriptionAndConceptLinks(val updatedMedia: MediaItem): MediaProcessingJob
}