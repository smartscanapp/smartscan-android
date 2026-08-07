package com.fpf.smartscan.core.search

import com.fpf.smartscan.core.media.MediaType

data class SearchFilter(
    val similarity: Float? = null,
    val mediaType: MediaType? = null,
    val tag: String? = null,
    val startDate: Long? = null,
    val endDate: Long? = null,
    val ids: List<Long> = emptyList(),
    val isDuplicate: Boolean? = null,
)

