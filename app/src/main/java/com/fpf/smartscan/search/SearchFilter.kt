package com.fpf.smartscan.search

import com.fpf.smartscan.media.MediaType

data class SearchFilter(
    val mediaType: MediaType? = null,
    val tag: String? = null,
    val startDate: Long? = null,
    val endDate: Long? = null,
)

