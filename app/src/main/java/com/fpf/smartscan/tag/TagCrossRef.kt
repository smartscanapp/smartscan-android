package com.fpf.smartscan.tag

import com.fpf.smartscan.media.MediaType


data class TagCrossRef(
    val mediaId: Long,
    val mediaType: MediaType,
    val tagId: Long
)