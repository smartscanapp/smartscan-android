package com.fpf.smartscan.core.search

import com.fpf.smartscan.core.media.MediaFilter
import com.fpf.smartscan.core.media.MediaType

data class SearchFilter(
    val mediaType: MediaType? = null,
    val tag: String? = null,
    val startDate: Long? = null,
    val endDate: Long? = null,
    val ids: List<Long> = emptyList(),
    val isDuplicate: Boolean? = null,
)

fun SearchFilter.toMediaFilter() = MediaFilter(
    mediaType = mediaType,
    isDuplicate = isDuplicate
)