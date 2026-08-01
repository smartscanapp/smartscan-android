package com.fpf.smartscan.core.concepts

import com.fpf.smartscan.core.media.MediaType

data class ConceptCrossRef (
    val mediaId: Long,
    val conceptId: Long,
    val mediaType: MediaType,
    val similarity: Float
)
