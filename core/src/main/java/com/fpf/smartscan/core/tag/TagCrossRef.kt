package com.fpf.smartscan.core.tag

import com.fpf.smartscan.core.media.MediaType


data class TagCrossRef(
    val mediaId: Long,
    val mediaType: MediaType,
    val tagId: Long
)