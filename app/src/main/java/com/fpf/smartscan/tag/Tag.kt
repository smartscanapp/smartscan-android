package com.fpf.smartscan.tag

data class Tag(
    val id: Long,
    val name: String,
    val lastUsedAt: Long? = null,
)
