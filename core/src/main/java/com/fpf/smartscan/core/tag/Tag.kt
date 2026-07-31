package com.fpf.smartscan.core.tag

data class Tag(
    val id: Long,
    val name: String,
    val lastUsedAt: Long? = null,
)


data class NewTag(
    val name: String
)