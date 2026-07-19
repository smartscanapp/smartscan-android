package com.fpf.smartscan.queue.jobs

import com.fpf.smartscan.media.MediaItem
import com.fpf.smartscan.media.MediaType
import com.fpf.smartscansdk.core.embeddings.StoredEmbedding

sealed interface MediaProcessingJob{
    data class UpdateConceptLinks(val updatedEmbed: StoredEmbedding, val mediaType: MediaType): MediaProcessingJob
    data class UpdateDescriptionAndConceptLinks(val updatedMedia: MediaItem): MediaProcessingJob
}