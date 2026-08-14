package com.fpf.smartscan.core.media

data class MediaFilter(
    val mediaType: MediaType? = null,
    val isDuplicate: Boolean? = null,
    val showHidden: Boolean? = false,
    )
