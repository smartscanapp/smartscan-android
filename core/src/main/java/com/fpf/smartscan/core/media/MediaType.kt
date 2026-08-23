package com.fpf.smartscan.core.media

import kotlinx.serialization.Serializable

@Serializable
enum class MediaType(val code: Int) {
    IMAGE(0),
    VIDEO(1)
}

fun MediaType.format(): String = name.lowercase().replaceFirstChar {it.uppercase() }