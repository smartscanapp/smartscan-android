package com.fpf.smartscan.concepts

import com.fpf.smartscan.media.MediaType

data class ConceptCrossRef (
    val mediaId: Long,
    val conceptId: Long,
    val mediaType: MediaType,
    val similarity: Float
)
